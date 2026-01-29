package com.enterprise.memo.service;

import com.enterprise.memo.dto.CreateCategoryRequest;
import com.enterprise.memo.dto.CreateTopicRequest;
import com.enterprise.memo.entity.MemoCategory;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoCategoryRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemoConfigurationService {

    private final MemoCategoryRepository categoryRepository;
    private final MemoTopicRepository topicRepository;
    private final com.enterprise.memo.client.WorkflowClient workflowClient;
    private final WorkflowConfigService workflowConfigService;

    public MemoCategory createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + request.getCode());
        }

        MemoCategory category = MemoCategory.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .accessPolicy(request.getAccessPolicy())
                .build();

        return categoryRepository.save(category);
    }

    public List<MemoCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public MemoTopic createTopic(CreateTopicRequest request) {
        MemoCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (topicRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Topic code already exists: " + request.getCode());
        }

        MemoTopic topic = MemoTopic.builder()
                .category(category)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .workflowTemplateId(request.getWorkflowTemplateId())
                .formDefinitionId(request.getFormDefinitionId())
                .contentTemplate(request.getContentTemplate())
                .numberingPattern(request.getNumberingPattern())
                .build();

        return topicRepository.save(topic);
    }

    public List<MemoTopic> getTopicsByCategory(String categoryId) {
        return topicRepository.findByCategoryId(java.util.UUID.fromString(categoryId));
    }

    public MemoTopic getTopic(java.util.UUID topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
    }

    public MemoTopic updateTopicWorkflow(java.util.UUID topicId, String workflowXml) {
        MemoTopic topic = getTopic(topicId);
        topic.setWorkflowXml(workflowXml);
        return topicRepository.save(topic);
    }

    public MemoTopic updateTopicFormSchema(java.util.UUID topicId, java.util.Map<String, Object> formSchema) {
        MemoTopic topic = getTopic(topicId);
        topic.setFormSchema(formSchema);
        return topicRepository.save(topic);
    }

    public MemoTopic updateTopicWorkflowTemplate(java.util.UUID topicId, String workflowTemplateId) {
        MemoTopic topic = getTopic(topicId);
        topic.setWorkflowTemplateId(workflowTemplateId);
        return topicRepository.save(topic);
    }

    /**
     * Deploy the topic's workflow BPMN to the Flowable engine.
     * Creates a ProcessTemplate in workflow-service and links the resulting
     * processTemplateId back to the topic for centralized assignment resolution.
     * 
     * BPMN is enriched with task listeners before deployment to enable
     * dynamic assignment via webhook callbacks.
     */
    public MemoTopic deployTopicWorkflow(java.util.UUID topicId) {
        MemoTopic topic = getTopic(topicId);

        if (topic.getWorkflowXml() == null || topic.getWorkflowXml().isBlank()) {
            throw new IllegalStateException("No workflow BPMN defined for topic: " + topic.getName());
        }

        log.info("Deploying workflow for topic: {} ({})", topic.getName(), topicId);

        // Enrich BPMN with task listeners and branching conditions
        java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs = workflowConfigService
                .getStepConfigs(topicId);
        String enrichedXml = com.enterprise.memo.util.BpmnEnricher.enrichBpmn(topic.getWorkflowXml(), stepConfigs);

        // Call workflow-service to deploy the enriched BPMN
        com.enterprise.memo.client.WorkflowClient.BpmnDeployResult deployResult = workflowClient.deployBpmn(
                topic.getCode(),
                topic.getName() + " Workflow",
                enrichedXml);

        // Store the ProcessTemplate UUID (for centralized assignment resolution)
        // The Flowable processDefinitionId is in the ProcessTemplate record
        topic.setWorkflowTemplateId(deployResult.processTemplateId());
        log.info("Workflow deployed successfully. ProcessTemplateId: {}, ProcessDefinitionId: {}",
                deployResult.processTemplateId(), deployResult.processDefinitionId());

        return topicRepository.save(topic);
    }

    /**
     * Update memo-wide viewer configuration for a topic.
     */
    public MemoTopic updateTopicViewers(java.util.UUID topicId, java.util.Map<String, Object> viewerConfig) {
        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        topic.setViewerConfig(viewerConfig);
        log.info("Updated viewer configuration for topic: {}", topicId);

        return topicRepository.save(topic);
    }

    /**
     * Update override permissions for a topic.
     * Controls what users can customize when creating memos (assignments, SLAs,
     * etc.)
     */
    public MemoTopic updateTopicOverridePermissions(java.util.UUID topicId,
            java.util.Map<String, Object> overridePermissions) {
        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        topic.setOverridePermissions(overridePermissions);
        log.info("Updated override permissions for topic: {}", topicId);

        return topicRepository.save(topic);
    }

    /**
     * Get all available workflow variables for a topic.
     * Used by the condition builder UI to populate variable dropdowns.
     * Returns variables from: memo fields, topic form schema, and initiator
     * context.
     */
    @Transactional(readOnly = true)
    public List<com.enterprise.memo.dto.WorkflowVariable> getWorkflowVariables(java.util.UUID topicId) {
        MemoTopic topic = getTopic(topicId);
        java.util.List<com.enterprise.memo.dto.WorkflowVariable> variables = new java.util.ArrayList<>();

        // 1. Standard memo fields (always available)
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("memo.subject").label("Memo Subject").type("text").source("memo").build());
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("memo.priority").label("Priority").type("enum").source("memo")
                .options(List.of("LOW", "MEDIUM", "HIGH", "URGENT")).build());
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("memo.dueDate").label("Due Date").type("date").source("memo").build());
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("memo.attachmentCount").label("Number of Attachments").type("number").source("memo").build());

        // 2. Initiator context (always available)
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("initiator.role").label("Initiator Role").type("enum").source("initiator")
                .options(List.of("OFFICER", "MANAGER", "DIRECTOR", "VP", "CEO")).build());
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("initiator.department").label("Initiator Department").type("enum").source("initiator")
                .options(List.of("IT", "HR", "FINANCE", "OPERATIONS", "MARKETING", "LEGAL")).build());
        variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                .name("initiator.branch").label("Initiator Branch").type("text").source("initiator").build());

        // 3. Form fields from topic's form schema (dynamic per topic)
        if (topic.getFormSchema() != null && !topic.getFormSchema().isEmpty()) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> fields = (java.util.List<java.util.Map<String, Object>>) topic
                    .getFormSchema().get("fields");

            if (fields != null) {
                for (java.util.Map<String, Object> field : fields) {
                    String name = (String) field.get("name");
                    String label = (String) field.get("label");
                    String type = mapFormTypeToConditionType((String) field.get("type"));

                    var varBuilder = com.enterprise.memo.dto.WorkflowVariable.builder()
                            .name("form." + name)
                            .label(label != null ? label : name)
                            .type(type)
                            .source("form");

                    // For select/enum types, include options
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, String>> options = (java.util.List<java.util.Map<String, String>>) field
                            .get("options");
                    if (options != null) {
                        varBuilder.options(options.stream()
                                .map(opt -> opt.get("value"))
                                .filter(java.util.Objects::nonNull)
                                .toList());
                    }

                    variables.add(varBuilder.build());
                }
            }
        }

        log.debug("Returning {} workflow variables for topic: {}", variables.size(), topicId);
        return variables;
    }

    /**
     * Map form field types to condition variable types.
     */
    private String mapFormTypeToConditionType(String formType) {
        if (formType == null)
            return "text";
        return switch (formType.toLowerCase()) {
            case "number", "currency", "decimal", "integer" -> "number";
            case "select", "radio", "dropdown" -> "enum";
            case "date", "datetime" -> "date";
            case "checkbox", "toggle" -> "boolean";
            default -> "text";
        };
    }
}
