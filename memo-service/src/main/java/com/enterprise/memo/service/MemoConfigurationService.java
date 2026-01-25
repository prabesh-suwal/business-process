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
     * Creates a process template and links the resulting templateId back to the
     * topic.
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

        // Enrich BPMN with task listeners for dynamic assignment
        String enrichedXml = com.enterprise.memo.util.BpmnEnricher.enrichBpmn(topic.getWorkflowXml());

        // Call workflow-service to deploy the enriched BPMN
        String templateId = workflowClient.deployBpmn(
                topic.getCode(),
                topic.getName() + " Workflow",
                enrichedXml);

        // Store the template ID
        topic.setWorkflowTemplateId(templateId);
        log.info("Workflow deployed successfully. Template ID: {}", templateId);

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
}
