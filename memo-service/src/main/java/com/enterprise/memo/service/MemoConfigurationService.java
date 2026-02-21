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
        private final com.enterprise.memo.repository.WorkflowVersionHistoryRepository versionHistoryRepository;

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

                // Reject edit if workflow is already deployed (locked)
                if (topic.isWorkflowDeployed()) {
                        throw new IllegalStateException(
                                        "Cannot modify deployed workflow. Use 'Copy to New Version' to create an editable copy.");
                }

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
        public MemoTopic deployTopicWorkflow(java.util.UUID topicId, String productId) {
                MemoTopic topic = getTopic(topicId);

                if (topic.getWorkflowXml() == null || topic.getWorkflowXml().isBlank()) {
                        throw new IllegalStateException("No workflow BPMN defined for topic: " + topic.getName());
                }

                log.info("Deploying workflow for topic: {} ({}) with productId: {}", topic.getName(), topicId,
                                productId);

                // Enrich BPMN with task listeners and branching conditions
                java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs = workflowConfigService
                                .getStepConfigs(topicId);
                String enrichedXml = com.enterprise.memo.util.BpmnEnricher.enrichBpmn(topic.getWorkflowXml(),
                                stepConfigs);

                // Call workflow-service to deploy the enriched BPMN
                com.enterprise.memo.client.WorkflowClient.BpmnDeployResult deployResult = workflowClient.deployBpmn(
                                topic.getCode(),
                                topic.getName() + " Workflow",
                                enrichedXml,
                                productId);

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

                // Reject edit if workflow is already deployed (locked)
                if (topic.isWorkflowDeployed()) {
                        throw new IllegalStateException(
                                        "Cannot modify deployed workflow. Use 'Copy to New Version' to create an editable copy.");
                }

                topic.setOverridePermissions(overridePermissions);
                log.info("Updated override permissions for topic: {}", topicId);

                return topicRepository.save(topic);
        }

        /**
         * Update default assignee configuration for a topic.
         * These rules apply to workflow steps that don't have specific assignment
         * config.
         */
        public MemoTopic updateTopicDefaultAssignee(java.util.UUID topicId,
                        java.util.Map<String, Object> assigneeConfig) {
                MemoTopic topic = topicRepository.findById(topicId)
                                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

                topic.setDefaultAssigneeConfig(assigneeConfig);
                log.info("Updated default assignee configuration for topic: {}", topicId);

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
                                .name("memo.dueDate").label(" ").type("date").source("memo").build());
                variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                                .name("memo.attachmentCount").label("Number of Attachments").type("number")
                                .source("memo").build());

                // 2. Initiator context (always available)
                variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                                .name("initiator.role").label("Initiator Role").type("enum").source("initiator")
                                .options(List.of("OFFICER", "MANAGER", "DIRECTOR", "VP", "CEO")).build());
                variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                                .name("initiator.department").label("Initiator Department").type("enum")
                                .source("initiator")
                                .options(List.of("IT", "HR", "FINANCE", "OPERATIONS", "MARKETING", "LEGAL")).build());
                variables.add(com.enterprise.memo.dto.WorkflowVariable.builder()
                                .name("initiator.branch").label("Initiator Branch").type("text").source("initiator")
                                .build());

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

        /**
         * Copy workflow to a new version.
         * BEFORE incrementing version, snapshots current deployed state to history
         * table.
         * This preserves the processTemplateId for running memo instances.
         * 
         * Creates a new editable copy of the deployed workflow with:
         * - Snapshot of current version saved to workflow_version_history
         * - Incremented version number
         * - Cleared workflow_template_id (draft state)
         * - Copied workflow_xml
         * - Step configs and gateway rules carry forward (topic-bound)
         * 
         * @param topicId The topic to copy workflow from
         * @return The topic with new version (draft, editable)
         */
        public MemoTopic copyTopicWorkflow(java.util.UUID topicId) {
                MemoTopic topic = getTopic(topicId);

                if (!topic.isWorkflowDeployed()) {
                        throw new IllegalStateException(
                                        "Workflow is not deployed. Edit the current draft instead of copying.");
                }

                int currentVersion = topic.getWorkflowVersion() != null ? topic.getWorkflowVersion() : 1;

                log.info("Snapshotting workflow version {} for topic: {} before creating new version",
                                currentVersion, topic.getName());

                // Snapshot current deployed version to history
                snapshotCurrentVersion(topic, currentVersion);

                // Increment version on topic
                int newVersion = currentVersion + 1;
                topic.setWorkflowVersion(newVersion);

                // Clear workflow template ID (makes it draft/editable)
                // The old templateId is now preserved in the version history
                topic.setWorkflowTemplateId(null);
                // workflow_xml is kept as-is for editing

                topic = topicRepository.save(topic);
                log.info("Created workflow version {} for topic: {} (now editable). Version {} preserved in history.",
                                newVersion, topic.getName(), currentVersion);

                return topic;
        }

        /**
         * Snapshot the current deployed workflow version to history table.
         */
        private void snapshotCurrentVersion(MemoTopic topic, int version) {
                // Check if this version is already snapshotted
                if (versionHistoryRepository.findByTopicIdAndVersion(topic.getId(), version).isPresent()) {
                        log.info("Version {} already snapshotted for topic {}", version, topic.getId());
                        return;
                }

                // Get current step configs
                java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs = workflowConfigService
                                .getStepConfigs(topic.getId());

                // Convert step configs to snapshot format
                java.util.Map<String, Object> stepConfigsSnapshot = new java.util.HashMap<>();
                for (var config : stepConfigs) {
                        java.util.Map<String, Object> configMap = new java.util.HashMap<>();
                        configMap.put("taskName", config.getTaskName());
                        configMap.put("assignmentConfig", config.getAssignmentConfig());
                        configMap.put("slaConfig", config.getSlaConfig());
                        configMap.put("escalationConfig", config.getEscalationConfig());
                        configMap.put("viewerConfig", config.getViewerConfig());
                        configMap.put("conditionConfig", config.getConditionConfig());
                        stepConfigsSnapshot.put(config.getTaskKey(), configMap);
                }

                // Merge outcomeConfig from workflow-service task configurations
                if (topic.getWorkflowTemplateId() != null) {
                        try {
                                java.util.List<java.util.Map<String, Object>> taskConfigs = workflowClient
                                                .getTaskConfigsForTemplate(topic.getWorkflowTemplateId());
                                for (java.util.Map<String, Object> tc : taskConfigs) {
                                        String taskKey = (String) tc.get("taskKey");
                                        // Read from dedicated outcomeConfig field (new) or legacy config.outcomeConfig
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<String, Object> outcomeConfig = (java.util.Map<String, Object>) tc
                                                        .get("outcomeConfig");
                                        if (outcomeConfig == null) {
                                                // Fallback: check legacy config map
                                                @SuppressWarnings("unchecked")
                                                java.util.Map<String, Object> tcConfig = (java.util.Map<String, Object>) tc
                                                                .get("config");
                                                if (tcConfig != null && tcConfig.containsKey("outcomeConfig")) {
                                                        @SuppressWarnings("unchecked")
                                                        java.util.Map<String, Object> legacyOC = (java.util.Map<String, Object>) tcConfig
                                                                        .get("outcomeConfig");
                                                        outcomeConfig = legacyOC;
                                                }
                                        }
                                        if (taskKey != null && outcomeConfig != null) {
                                                java.util.Map<String, Object> stepSnap = (java.util.Map<String, Object>) stepConfigsSnapshot
                                                                .computeIfAbsent(taskKey,
                                                                                k -> new java.util.HashMap<>());
                                                stepSnap.put("outcomeConfig", outcomeConfig);
                                        }
                                }
                                log.debug("Merged outcome configs from {} task configurations", taskConfigs.size());
                        } catch (Exception e) {
                                log.warn("Could not fetch task configs for outcome snapshot: {}", e.getMessage());
                        }
                }

                // Get current gateway rules
                java.util.List<com.enterprise.memo.entity.GatewayDecisionRule> gatewayRules = workflowConfigService
                                .getGatewayRules(topic.getId());

                // Convert gateway rules to snapshot format
                java.util.Map<String, Object> gatewayRulesSnapshot = new java.util.HashMap<>();
                for (var rule : gatewayRules) {
                        java.util.Map<String, Object> ruleMap = new java.util.HashMap<>();
                        ruleMap.put("gatewayKey", rule.getGatewayKey());
                        ruleMap.put("gatewayName", rule.getGatewayName());
                        ruleMap.put("rules", rule.getRules());
                        ruleMap.put("defaultFlow", rule.getDefaultFlow());
                        ruleMap.put("version", rule.getVersion());
                        ruleMap.put("active", rule.getActive());
                        gatewayRulesSnapshot.put(rule.getGatewayKey(), ruleMap);
                }

                // Create version history record
                com.enterprise.memo.entity.WorkflowVersionHistory history = com.enterprise.memo.entity.WorkflowVersionHistory
                                .builder()
                                .topicId(topic.getId())
                                .version(version)
                                .workflowXml(topic.getWorkflowXml())
                                .workflowTemplateId(topic.getWorkflowTemplateId())
                                .stepConfigsSnapshot(stepConfigsSnapshot)
                                .gatewayRulesSnapshot(gatewayRulesSnapshot)
                                .deployedAt(java.time.LocalDateTime.now())
                                .build();

                versionHistoryRepository.save(history);
                log.info("Snapshotted version {} with processTemplateId {} for topic {}",
                                version, topic.getWorkflowTemplateId(), topic.getName());
        }

        /**
         * Get workflow version history for a topic.
         */
        public java.util.List<com.enterprise.memo.entity.WorkflowVersionHistory> getWorkflowVersions(
                        java.util.UUID topicId) {
                return versionHistoryRepository.findByTopicIdOrderByVersionDesc(topicId);
        }

        /**
         * Get a specific workflow version.
         */
        public com.enterprise.memo.entity.WorkflowVersionHistory getWorkflowVersion(java.util.UUID topicId,
                        int version) {
                return versionHistoryRepository.findByTopicIdAndVersion(topicId, version)
                                .orElseThrow(() -> new RuntimeException(
                                                "Workflow version " + version + " not found for topic " + topicId));
        }
}
