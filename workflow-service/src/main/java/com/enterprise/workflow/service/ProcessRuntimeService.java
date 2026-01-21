package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.ProcessInstanceMetadata;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing process instances (start, query, cancel).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessRuntimeService {

    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessInstanceMetadataRepository processInstanceMetadataRepository;
    private final ActionTimelineRepository actionTimelineRepository;
    private final RuntimeService runtimeService;

    /**
     * Start a new process instance from a process template.
     */
    public ProcessInstanceDTO startProcess(StartProcessRequest request, UUID startedBy, String startedByName) {
        ProcessTemplate template = processTemplateRepository.findById(request.getProcessTemplateId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Template not found: " + request.getProcessTemplateId()));

        if (template.getStatus() != ProcessTemplate.ProcessTemplateStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot start process from non-ACTIVE template. Status: " + template.getStatus());
        }

        if (template.getFlowableProcessDefKey() == null) {
            throw new IllegalStateException("Template has not been deployed to Flowable");
        }

        // Start Flowable process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                template.getFlowableProcessDefKey(),
                request.getBusinessKey(),
                request.getVariables() != null ? request.getVariables() : Map.of());

        // Create our metadata record
        ProcessInstanceMetadata metadata = ProcessInstanceMetadata.builder()
                .flowableProcessInstanceId(processInstance.getId())
                .processTemplate(template)
                .productId(template.getProductId())
                .businessKey(request.getBusinessKey())
                .title(request.getTitle())
                .startedBy(startedBy)
                .startedByName(startedByName)
                .status(ProcessInstanceMetadata.ProcessInstanceStatus.RUNNING)
                .build();

        metadata = processInstanceMetadataRepository.save(metadata);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(processInstance.getId())
                .actionType(ActionTimeline.ActionType.PROCESS_STARTED)
                .actorId(startedBy)
                .actorName(startedByName)
                .metadata(Map.of(
                        "templateId", template.getId().toString(),
                        "templateName", template.getName(),
                        "businessKey", request.getBusinessKey() != null ? request.getBusinessKey() : ""))
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Started process instance {} for template {} by user {}",
                processInstance.getId(), template.getName(), startedByName);

        return toDTO(metadata, template);
    }

    /**
     * Get process instance by Flowable ID.
     */
    @Transactional(readOnly = true)
    public ProcessInstanceDTO getProcessInstance(String flowableProcessInstanceId) {
        ProcessInstanceMetadata metadata = processInstanceMetadataRepository
                .findByFlowableProcessInstanceId(flowableProcessInstanceId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Process instance not found: " + flowableProcessInstanceId));

        return toDTO(metadata, metadata.getProcessTemplate());
    }

    /**
     * Get process instances by product.
     */
    @Transactional(readOnly = true)
    public Page<ProcessInstanceDTO> getProcessInstancesByProduct(UUID productId, Pageable pageable) {
        return processInstanceMetadataRepository
                .findByProductIdOrderByStartedAtDesc(productId, pageable)
                .map(m -> toDTO(m, m.getProcessTemplate()));
    }

    /**
     * Get process instances started by a user.
     */
    @Transactional(readOnly = true)
    public Page<ProcessInstanceDTO> getProcessInstancesByUser(UUID userId, Pageable pageable) {
        return processInstanceMetadataRepository
                .findByStartedByOrderByStartedAtDesc(userId, pageable)
                .map(m -> toDTO(m, m.getProcessTemplate()));
    }

    /**
     * Cancel a running process instance.
     */
    public void cancelProcess(String flowableProcessInstanceId, UUID cancelledBy, String cancelledByName,
            String reason) {
        ProcessInstanceMetadata metadata = processInstanceMetadataRepository
                .findByFlowableProcessInstanceId(flowableProcessInstanceId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Process instance not found: " + flowableProcessInstanceId));

        // Delete from Flowable
        runtimeService.deleteProcessInstance(flowableProcessInstanceId, reason);

        // Update our metadata
        metadata.setStatus(ProcessInstanceMetadata.ProcessInstanceStatus.CANCELLED);
        processInstanceMetadataRepository.save(metadata);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(flowableProcessInstanceId)
                .actionType(ActionTimeline.ActionType.PROCESS_CANCELLED)
                .actorId(cancelledBy)
                .actorName(cancelledByName)
                .metadata(Map.of("reason", reason != null ? reason : ""))
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Cancelled process instance {} by user {}. Reason: {}",
                flowableProcessInstanceId, cancelledByName, reason);
    }

    /**
     * Get variables for a process instance.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProcessVariables(String flowableProcessInstanceId) {
        return runtimeService.getVariables(flowableProcessInstanceId);
    }

    /**
     * Set a variable on a process instance.
     */
    public void setProcessVariable(String flowableProcessInstanceId, String variableName, Object value) {
        runtimeService.setVariable(flowableProcessInstanceId, variableName, value);
        log.debug("Set variable {} on process {}", variableName, flowableProcessInstanceId);
    }

    private ProcessInstanceDTO toDTO(ProcessInstanceMetadata metadata, ProcessTemplate template) {
        return ProcessInstanceDTO.builder()
                .id(metadata.getId())
                .flowableProcessInstanceId(metadata.getFlowableProcessInstanceId())
                .processTemplateId(template != null ? template.getId() : null)
                .processTemplateName(template != null ? template.getName() : null)
                .productId(metadata.getProductId())
                .businessKey(metadata.getBusinessKey())
                .title(metadata.getTitle())
                .startedBy(metadata.getStartedBy())
                .startedByName(metadata.getStartedByName())
                .startedAt(metadata.getStartedAt())
                .completedAt(metadata.getCompletedAt())
                .status(metadata.getStatus())
                .priority(metadata.getPriority())
                .dueDate(metadata.getDueDate())
                .build();
    }
}
