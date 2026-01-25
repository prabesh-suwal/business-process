package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.ProcessInstanceMetadata;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.ProcessTemplateForm;
import com.enterprise.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing process template CRUD and deployment to Flowable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessDesignService {

        private final ProcessTemplateRepository processTemplateRepository;
        private final ProcessTemplateFormRepository processTemplateFormRepository;
        private final ActionTimelineRepository actionTimelineRepository;
        private final RepositoryService repositoryService;

        /**
         * Create a new process template (DRAFT status).
         */
        public ProcessTemplateDTO createTemplate(CreateProcessTemplateRequest request, UUID createdBy) {
                // Check for existing template with same name
                Integer maxVersion = processTemplateRepository
                                .findMaxVersionByProductIdAndName(request.getProductId(), request.getName())
                                .orElse(0);

                ProcessTemplate template = ProcessTemplate.builder()
                                .productId(request.getProductId())
                                .name(request.getName())
                                .description(request.getDescription())
                                .bpmnXml(request.getBpmnXml())
                                .version(maxVersion + 1)
                                .status(ProcessTemplate.ProcessTemplateStatus.DRAFT)
                                .createdBy(createdBy)
                                .build();

                template = processTemplateRepository.save(template);
                log.info("Created process template: {} (version {})", template.getName(), template.getVersion());

                return toDTO(template);
        }

        /**
         * Update an existing process template (only DRAFT templates can be updated).
         */
        public ProcessTemplateDTO updateTemplate(UUID templateId, UpdateProcessTemplateRequest request) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

                if (template.getStatus() != ProcessTemplate.ProcessTemplateStatus.DRAFT) {
                        throw new IllegalStateException(
                                        "Only DRAFT templates can be updated. Current status: " + template.getStatus());
                }

                if (request.getName() != null) {
                        template.setName(request.getName());
                }
                if (request.getDescription() != null) {
                        template.setDescription(request.getDescription());
                }
                if (request.getBpmnXml() != null) {
                        template.setBpmnXml(request.getBpmnXml());
                }

                template = processTemplateRepository.save(template);
                log.info("Updated process template: {}", template.getId());

                return toDTO(template);
        }

        /**
         * Deploy a process template to Flowable engine.
         * This makes the template ACTIVE and creates a Flowable process definition.
         */
        public ProcessTemplateDTO deployTemplate(UUID templateId, UUID deployedBy) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

                if (template.getBpmnXml() == null || template.getBpmnXml().isBlank()) {
                        throw new IllegalStateException("Cannot deploy template without BPMN XML");
                }

                // Generate process definition key from template name
                String processDefKey = generateProcessDefKey(template);

                // Deploy to Flowable
                Deployment deployment = repositoryService.createDeployment()
                                .name(template.getName() + " v" + template.getVersion())
                                .addInputStream(
                                                processDefKey + ".bpmn20.xml",
                                                new ByteArrayInputStream(
                                                                template.getBpmnXml().getBytes(StandardCharsets.UTF_8)))
                                .deploy();

                // Get the deployed process definition
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                                .deploymentId(deployment.getId())
                                .singleResult();

                // Update template with Flowable references
                template.setFlowableDeploymentId(deployment.getId());
                template.setFlowableProcessDefKey(processDefinition.getKey());
                template.setStatus(ProcessTemplate.ProcessTemplateStatus.ACTIVE);

                template = processTemplateRepository.save(template);

                log.info("Deployed process template {} to Flowable. Deployment ID: {}, Process Definition Key: {}",
                                template.getName(), deployment.getId(), processDefinition.getKey());

                return toDTO(template);
        }

        /**
         * Deploy raw BPMN XML directly to Flowable engine.
         * This bypasses template management and is used for topic-specific workflows.
         * Returns the Flowable process definition ID.
         */
        public String deployRawBpmn(String processKey, String processName, String bpmnXml) {
                if (bpmnXml == null || bpmnXml.isBlank()) {
                        throw new IllegalArgumentException("BPMN XML is required");
                }

                // Normalize the process key
                String normalizedKey = processKey.toLowerCase()
                                .replaceAll("[^a-z0-9]", "_")
                                .replaceAll("_+", "_")
                                .replaceAll("^_|_$", "");

                log.info("Deploying raw BPMN for process: {} (key: {})", processName, normalizedKey);

                // Deploy to Flowable
                Deployment deployment = repositoryService.createDeployment()
                                .name(processName != null ? processName : normalizedKey)
                                .addInputStream(
                                                normalizedKey + ".bpmn20.xml",
                                                new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                                .deploy();

                // Get the deployed process definition
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                                .deploymentId(deployment.getId())
                                .singleResult();

                log.info("Deployed raw BPMN successfully. Deployment ID: {}, Process Definition: {} ({})",
                                deployment.getId(), processDefinition.getKey(), processDefinition.getId());

                // Return the process definition ID (this is what memo-service needs to start
                // instances)
                return processDefinition.getId();
        }

        /**
         * Get a process template by ID.
         */
        @Transactional(readOnly = true)
        public ProcessTemplateDTO getTemplate(UUID templateId) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
                return toDTO(template);
        }

        /**
         * Get all process templates for a product.
         */
        @Transactional(readOnly = true)
        public List<ProcessTemplateDTO> getTemplatesByProduct(UUID productId) {
                return processTemplateRepository.findByProductIdOrderByNameAsc(productId)
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /**
         * Get active process templates for a product.
         */
        @Transactional(readOnly = true)
        public List<ProcessTemplateDTO> getActiveTemplatesByProduct(UUID productId) {
                return processTemplateRepository
                                .findByProductIdAndStatusOrderByNameAsc(productId,
                                                ProcessTemplate.ProcessTemplateStatus.ACTIVE)
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /**
         * Get all active process templates across all products.
         */
        @Transactional(readOnly = true)
        public List<ProcessTemplateDTO> getAllActiveTemplates() {
                return processTemplateRepository
                                .findByStatusOrderByNameAsc(ProcessTemplate.ProcessTemplateStatus.ACTIVE)
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /**
         * Get ALL process templates across all products (including DRAFTS).
         */
        @Transactional(readOnly = true)
        public List<ProcessTemplateDTO> getAllTemplates() {
                return processTemplateRepository.findAll().stream()
                                .map(this::toDTO)
                                .toList();
        }

        /**
         * Delete a process template (only DRAFT templates can be deleted).
         */
        public void deleteTemplate(UUID templateId) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

                if (template.getStatus() != ProcessTemplate.ProcessTemplateStatus.DRAFT) {
                        throw new IllegalStateException(
                                        "Only DRAFT templates can be deleted. Current status: " + template.getStatus());
                }

                // Delete form mappings first
                processTemplateFormRepository.deleteByProcessTemplateId(templateId);

                processTemplateRepository.delete(template);
                log.info("Deleted process template: {}", templateId);
        }

        /**
         * Deprecate an ACTIVE process template.
         */
        public ProcessTemplateDTO deprecateTemplate(UUID templateId) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

                if (template.getStatus() != ProcessTemplate.ProcessTemplateStatus.ACTIVE) {
                        throw new IllegalStateException(
                                        "Only ACTIVE templates can be deprecated. Current status: "
                                                        + template.getStatus());
                }

                template.setStatus(ProcessTemplate.ProcessTemplateStatus.DEPRECATED);
                template = processTemplateRepository.save(template);
                log.info("Deprecated process template: {}", templateId);

                return toDTO(template);
        }

        /**
         * Map form to a task in the process template.
         */
        public ProcessTemplateFormDTO mapFormToTask(UUID templateId, String taskKey, UUID formDefinitionId,
                        ProcessTemplateForm.FormType formType) {
                ProcessTemplate template = processTemplateRepository.findById(templateId)
                                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

                // Check if mapping already exists
                processTemplateFormRepository.findByProcessTemplateIdAndTaskKey(templateId, taskKey)
                                .ifPresent(existing -> {
                                        throw new IllegalStateException(
                                                        "Form already mapped to task " + taskKey
                                                                        + ". Delete existing mapping first.");
                                });

                ProcessTemplateForm mapping = ProcessTemplateForm.builder()
                                .processTemplate(template)
                                .taskKey(taskKey)
                                .formDefinitionId(formDefinitionId)
                                .formType(formType)
                                .build();

                mapping = processTemplateFormRepository.save(mapping);
                log.info("Mapped form {} to task {} in template {}", formDefinitionId, taskKey, templateId);

                return toFormDTO(mapping);
        }

        /**
         * Get all form mappings for a process template.
         */
        @Transactional(readOnly = true)
        public List<ProcessTemplateFormDTO> getFormsForTemplate(UUID templateId) {
                // Verify template exists
                if (!processTemplateRepository.existsById(templateId)) {
                        throw new IllegalArgumentException("Template not found: " + templateId);
                }

                return processTemplateFormRepository.findByProcessTemplateId(templateId)
                                .stream()
                                .map(this::toFormDTO)
                                .toList();
        }

        /**
         * Get the form mapped to a specific task in a process template.
         */
        @Transactional(readOnly = true)
        public ProcessTemplateFormDTO getFormForTask(UUID templateId, String taskKey) {
                return processTemplateFormRepository.findByProcessTemplateIdAndTaskKey(templateId, taskKey)
                                .map(this::toFormDTO)
                                .orElse(null);
        }

        /**
         * Remove form mapping from a task.
         */
        public void detachForm(UUID templateId, String taskKey) {
                ProcessTemplateForm mapping = processTemplateFormRepository
                                .findByProcessTemplateIdAndTaskKey(templateId, taskKey)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No form mapped to task " + taskKey + " in template " + templateId));

                processTemplateFormRepository.delete(mapping);
                log.info("Removed form mapping from task {} in template {}", taskKey, templateId);
        }

        private String generateProcessDefKey(ProcessTemplate template) {
                // Convert name to a valid process definition key
                String key = template.getName()
                                .toLowerCase()
                                .replaceAll("[^a-z0-9]", "_")
                                .replaceAll("_+", "_")
                                .replaceAll("^_|_$", "");
                return key + "_v" + template.getVersion();
        }

        private ProcessTemplateDTO toDTO(ProcessTemplate template) {
                return ProcessTemplateDTO.builder()
                                .id(template.getId())
                                .productId(template.getProductId())
                                .name(template.getName())
                                .description(template.getDescription())
                                .flowableProcessDefKey(template.getFlowableProcessDefKey())
                                .flowableDeploymentId(template.getFlowableDeploymentId())
                                .version(template.getVersion())
                                .status(template.getStatus())
                                .bpmnXml(template.getBpmnXml())
                                .createdBy(template.getCreatedBy())
                                .createdAt(template.getCreatedAt())
                                .updatedAt(template.getUpdatedAt())
                                .build();
        }

        private ProcessTemplateFormDTO toFormDTO(ProcessTemplateForm mapping) {
                return ProcessTemplateFormDTO.builder()
                                .id(mapping.getId())
                                .processTemplateId(mapping.getProcessTemplate().getId())
                                .taskKey(mapping.getTaskKey())
                                .formDefinitionId(mapping.getFormDefinitionId())
                                .formType(mapping.getFormType())
                                .createdAt(mapping.getCreatedAt())
                                .build();
        }

        /**
         * Create a new version of an existing process template.
         * This creates a new DRAFT template with the next version number,
         * copying the BPMN XML from the source template.
         */
        public ProcessTemplateDTO createNewVersion(UUID sourceTemplateId, UUID createdBy) {
                ProcessTemplate source = processTemplateRepository.findById(sourceTemplateId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Template not found: " + sourceTemplateId));

                // Find the next version number for this product/name combination
                Integer maxVersion = processTemplateRepository
                                .findMaxVersionByProductIdAndName(source.getProductId(), source.getName())
                                .orElse(0);

                ProcessTemplate newVersion = ProcessTemplate.builder()
                                .productId(source.getProductId())
                                .name(source.getName())
                                .description(source.getDescription())
                                .bpmnXml(source.getBpmnXml())
                                .version(maxVersion + 1)
                                .status(ProcessTemplate.ProcessTemplateStatus.DRAFT)
                                .createdBy(createdBy)
                                .build();

                newVersion = processTemplateRepository.save(newVersion);
                log.info("Created new version {} of template '{}' from source {}",
                                newVersion.getVersion(), newVersion.getName(), sourceTemplateId);

                return toDTO(newVersion);
        }
}
