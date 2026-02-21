package com.enterprise.memo.service;

import com.cas.common.logging.audit.AuditEventType;
import com.cas.common.logging.audit.AuditLogger;
import com.enterprise.memo.dto.CreateMemoRequest;
import com.enterprise.memo.dto.MemoDTO;
import com.enterprise.memo.dto.UpdateMemoRequest;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoStatus;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemoTopicRepository topicRepository;
    private final MemoNumberingService numberingService;
    private final com.enterprise.memo.client.WorkflowClient workflowClient;
    private final ViewerService viewerService;
    private final com.enterprise.memo.util.DynamicWorkflowGenerator dynamicWorkflowGenerator;
    private final AuditLogger auditLogger;

    // Mapper could be MapStruct, but implementing manual for now to save
    // time/complexity
    private MemoDTO toDTO(Memo memo) {
        MemoDTO dto = new MemoDTO();
        dto.setId(memo.getId());
        dto.setMemoNumber(memo.getMemoNumber());
        dto.setSubject(memo.getSubject());
        dto.setStatus(memo.getStatus());
        dto.setPriority(memo.getPriority());
        dto.setContent(memo.getContent());
        dto.setFormData(memo.getFormData());

        dto.setTopicId(memo.getTopic().getId());
        dto.setTopicName(memo.getTopic().getName());
        dto.setCategoryId(memo.getCategory().getId());
        dto.setCategoryName(memo.getCategory().getName());

        dto.setCreatedBy(memo.getCreatedBy());
        dto.setCreatedAt(memo.getCreatedAt());
        dto.setUpdatedAt(memo.getUpdatedAt());
        dto.setProcessInstanceId(memo.getProcessInstanceId());
        return dto;
    }

    @Transactional
    public MemoDTO createDraft(CreateMemoRequest request, UUID userId) {
        MemoTopic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        String memoNumber = numberingService.generateMemoNumber(topic);

        Memo memo = new Memo();
        memo.setMemoNumber(memoNumber);
        memo.setSubject(request.getSubject());
        memo.setTopic(topic);
        memo.setCategory(topic.getCategory());
        memo.setPriority(request.getPriority());
        memo.setStatus(MemoStatus.DRAFT);
        memo.setCreatedBy(userId);

        // Link memo to current workflow version
        memo.setWorkflowVersion(topic.getWorkflowVersion() != null ? topic.getWorkflowVersion() : 1);

        // Initialize content from template if available
        if (topic.getContentTemplate() != null) {
            memo.setContent(topic.getContentTemplate());
        }

        memo = memoRepository.save(memo);

        // Audit log with builder pattern
        auditLogger.log()
                .eventType(AuditEventType.CREATE)
                .action("Created new memo draft")
                .module("MEMO")
                .entity("MEMO", memo.getId().toString())
                .businessKey(memoNumber)
                .newValue(toDTO(memo))
                .success();

        return toDTO(memo);
    }

    @Transactional(readOnly = true)
    public MemoDTO getMemo(UUID id) {
        return memoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Memo not found"));
    }

    @Transactional(readOnly = true)
    public List<MemoDTO> getMyMemos(UUID userId) {
        return memoRepository.findByCreatedBy(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemoDTO updateMemo(UUID id, UpdateMemoRequest request) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        if (memo.getStatus() != MemoStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT memos can be edited");
        }

        // Capture old state
        MemoDTO oldState = toDTO(memo);

        if (request.getSubject() != null) {
            memo.setSubject(request.getSubject());
        }
        if (request.getPriority() != null) {
            memo.setPriority(request.getPriority());
        }
        if (request.getContent() != null) {
            memo.setContent(request.getContent());
        }
        if (request.getFormData() != null) {
            memo.setFormData(request.getFormData());
        }
        if (request.getWorkflowOverrides() != null) {
            memo.setWorkflowOverrides(request.getWorkflowOverrides());
        }

        memo = memoRepository.save(memo);

        // Audit log with builder pattern
        auditLogger.log()
                .eventType(AuditEventType.UPDATE)
                .action("Updated memo content")
                .module("MEMO")
                .entity("MEMO", id.toString())
                .businessKey(memo.getMemoNumber())
                .oldValue(oldState)
                .newValue(toDTO(memo))
                .success();

        return toDTO(memo);
    }

    @Transactional
    public MemoDTO submitMemo(UUID id, UUID userId) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        if (memo.getStatus() != MemoStatus.DRAFT && memo.getStatus() != MemoStatus.SENT_BACK) {
            throw new RuntimeException("Only DRAFT or SENT_BACK memos can be submitted");
        }

        // Get topic to get workflowTemplateId
        var topic = memo.getTopic();

        // Check if there are workflow overrides (custom workflow defined in memo)
        boolean hasWorkflowOverrides = memo.getWorkflowOverrides() != null
                && Boolean.TRUE.equals(memo.getWorkflowOverrides().get("customWorkflow"));

        if (topic == null || (topic.getWorkflowTemplateId() == null && !hasWorkflowOverrides)) {
            // For MVP, allow submission without workflow (just change status)
            log.warn("No workflow configured for topic and no custom workflow. Skipping workflow start.");
            memo.setStatus(MemoStatus.SUBMITTED);
            memo.setCurrentStage("Pending Review");
            memo = memoRepository.save(memo);
            return toDTO(memo);
        }

        // If there are workflow overrides, use dynamic workflow generation
        if (hasWorkflowOverrides) {
            return submitMemoWithDynamicWorkflow(memo, userId);
        }

        // Start Workflow
        try {
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            // Allow access via "memo.field" in expressions
            java.util.Map<String, Object> memoMap = new java.util.HashMap<>();
            memoMap.put("id", memo.getId().toString());
            memoMap.put("memoNumber", memo.getMemoNumber());
            memoMap.put("subject", memo.getSubject());
            memoMap.put("priority", memo.getPriority());
            if (memo.getFormData() != null) {
                memoMap.put("formData", memo.getFormData());
            }
            variables.put("memo", memoMap);

            variables.put("memoId", memo.getId().toString());
            variables.put("memoNumber", memo.getMemoNumber());
            variables.put("subject", memo.getSubject());
            variables.put("initiatorId", userId.toString());

            // Inject Workflow Configuration Variables (Dynamic Properties)
            // In a real system, these would be fetched from a DB configuration table or
            // properties service
            variables.put("approvalThreshold", 5000000L); // 50 Lakhs
            variables.put("escalationDelayLow", "P1D");
            variables.put("escalationDelayMedium", "P2D");
            variables.put("escalationDelayHigh", "P3D");

            // Dynamic Role Assignments
            variables.put("roleRM", "RELATIONSHIP_MANAGER");
            variables.put("roleBM", "BRANCH_MANAGER");
            variables.put("roleCreditAnalyst", "CREDIT_ANALYST");
            variables.put("roleRiskOfficer", "RISK_OFFICER");
            variables.put("roleApprover", "APPROVER");
            variables.put("roleDistrictHead", "DISTRICT_HEAD");
            variables.put("roleCreditCommitteeA", "CREDIT_COMMITTEE_A");

            // Committee Codes
            variables.put("committeeCreditA", "CC_A");

            // Add form data to variables if present
            if (memo.getFormData() != null) {
                variables.putAll(memo.getFormData());
            }

            com.enterprise.memo.dto.StartProcessRequest request = com.enterprise.memo.dto.StartProcessRequest.builder()
                    .processTemplateId(topic.getWorkflowTemplateId())
                    .businessKey(memo.getId().toString())
                    .title(memo.getMemoNumber() + " - " + memo.getSubject())
                    .variables(variables)
                    .build();

            String processInstanceId = workflowClient.startProcess(request, userId);
            memo.setProcessInstanceId(processInstanceId);
            log.info("Workflow started for memo {}, processInstanceId: {}", id, processInstanceId);

        } catch (Exception e) {
            log.error("Failed to start workflow for memo {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }

        memo.setStatus(MemoStatus.SUBMITTED);
        memo.setCurrentStage("Workflow Started");
        memo = memoRepository.save(memo);

        // Audit log with builder pattern
        auditLogger.log()
                .eventType(AuditEventType.SUBMIT)
                .action("Submitted memo for approval")
                .module("WORKFLOW")
                .entity("MEMO", id.toString())
                .businessKey(memo.getMemoNumber())
                .newValue(toDTO(memo))
                .success();

        return toDTO(memo);
    }

    /**
     * Submit memo with dynamically generated workflow from workflow overrides.
     * This is used when a topic doesn't have a pre-configured workflow but
     * the user defines custom approval steps.
     */
    @Transactional
    private MemoDTO submitMemoWithDynamicWorkflow(Memo memo, UUID userId) {
        log.info("Starting dynamic workflow for memo {} with workflow overrides", memo.getId());

        try {
            // Generate BPMN from workflow overrides
            String bpmnXml = dynamicWorkflowGenerator.generateBpmn(
                    memo.getWorkflowOverrides(),
                    memo.getId().toString());

            String processKey = dynamicWorkflowGenerator.generateProcessKey(memo.getId().toString());
            String processName = "Memo " + memo.getMemoNumber() + " Workflow";

            // Get product ID from topic category (or use a default)
            String productId = memo.getCategory() != null && memo.getCategory().getId() != null
                    ? memo.getCategory().getId().toString()
                    : "00000000-0000-0000-0000-000000000001"; // Default product ID

            // Deploy the dynamic BPMN to workflow-service
            log.info("Deploying dynamic BPMN for memo {}, processKey: {}", memo.getId(), processKey);
            var deployResult = workflowClient.deployBpmn(processKey, processName, bpmnXml, productId);

            if (deployResult == null || deployResult.processDefinitionId() == null) {
                throw new RuntimeException("Failed to deploy dynamic workflow - no process definition returned");
            }

            log.info("Dynamic workflow deployed: processDefinitionId={}, templateId={}",
                    deployResult.processDefinitionId(), deployResult.processTemplateId());

            // Store the template ID for future reference (don't update topic - it's
            // one-time per memo)
            // if (deployResult.processTemplateId() != null) {
            // memo.getTopic().setWorkflowTemplateId(deployResult.processTemplateId());
            // }

            // Prepare process variables
            java.util.Map<String, Object> variables = new java.util.HashMap<>();

            // Add memo data as nested object
            java.util.Map<String, Object> memoMap = new java.util.HashMap<>();
            memoMap.put("id", memo.getId().toString());
            memoMap.put("memoNumber", memo.getMemoNumber());
            memoMap.put("subject", memo.getSubject());
            memoMap.put("priority", memo.getPriority());
            if (memo.getFormData() != null) {
                memoMap.put("formData", memo.getFormData());
            }
            variables.put("memo", memoMap);

            // Add flat variables for backward compatibility
            variables.put("memoId", memo.getId().toString());
            variables.put("memoNumber", memo.getMemoNumber());
            variables.put("subject", memo.getSubject());
            variables.put("initiatorId", userId.toString());

            // Add form data if present
            if (memo.getFormData() != null) {
                variables.putAll(memo.getFormData());
            }

            // Start the workflow using the deployed process definition
            // Note: useProcessDefinitionId=true tells workflow-service to use
            // startProcessInstanceById
            com.enterprise.memo.dto.StartProcessRequest request = com.enterprise.memo.dto.StartProcessRequest.builder()
                    .processTemplateId(deployResult.processDefinitionId())
                    .useProcessDefinitionId(true) // Critical: tells service to use startProcessInstanceById
                    .businessKey(memo.getId().toString())
                    .title(memo.getMemoNumber() + " - " + memo.getSubject())
                    .variables(variables)
                    .build();

            String processInstanceId = workflowClient.startProcess(request, userId);
            memo.setProcessInstanceId(processInstanceId);

            log.info("Dynamic workflow started for memo {}", memo.getId());

        } catch (Exception e) {
            log.error("Failed to start dynamic workflow for memo {}: {}", memo.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to start dynamic workflow: " + e.getMessage(), e);
        }

        memo.setStatus(MemoStatus.SUBMITTED);
        memo.setCurrentStage("Workflow Started");
        memo = memoRepository.save(memo);
        return toDTO(memo);
    }

    @Transactional
    public void updateMemoStatus(UUID id, String statusStr) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        MemoStatus oldStatus = memo.getStatus();

        try {
            MemoStatus status = MemoStatus.valueOf(statusStr.toUpperCase());
            memo.setStatus(status);
            memo.setCurrentStage("Completed"); // Or handle strictly based on status
            memoRepository.save(memo);

            // Audit log with builder pattern
            auditLogger.log()
                    .eventType(AuditEventType.STATUS_CHANGE)
                    .action("Updated memo status")
                    .module("WORKFLOW")
                    .entity("MEMO", id.toString())
                    .businessKey(memo.getMemoNumber())
                    .remarks("Changed from " + oldStatus + " to " + status)
                    .success();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusStr);
        }
    }

    /**
     * Get memos that user can view (including view-only access).
     * Returns all memos user has permission to view, regardless of action
     * permission.
     */
    @Transactional(readOnly = true)
    public List<MemoDTO> getViewableMemos(String userId, List<String> roles, String departmentId) {
        log.debug("Getting viewable memos for user: {}, roles: {}, dept: {}", userId, roles, departmentId);

        List<UUID> viewableMemoIds = viewerService.getViewableMemos(userId, roles, departmentId);

        return viewableMemoIds.stream()
                .map(memoRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if user can view a specific memo.
     */
    @Transactional(readOnly = true)
    public boolean canViewMemo(UUID memoId, String userId, List<String> roles, String departmentId) {
        return viewerService.canViewMemo(memoId, userId, roles, departmentId);
    }
}
