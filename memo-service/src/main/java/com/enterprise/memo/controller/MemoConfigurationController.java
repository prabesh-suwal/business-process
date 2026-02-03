package com.enterprise.memo.controller;

import com.enterprise.memo.dto.CreateCategoryRequest;
import com.enterprise.memo.dto.CreateTopicRequest;
import com.enterprise.memo.entity.MemoCategory;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.service.MemoConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class MemoConfigurationController {

    private final MemoConfigurationService configurationService;

    @PostMapping("/categories")
    public ResponseEntity<MemoCategory> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(configurationService.createCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<MemoCategory>> getAllCategories() {
        return ResponseEntity.ok(configurationService.getAllCategories());
    }

    @PostMapping("/topics")
    public ResponseEntity<MemoTopic> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ResponseEntity.ok(configurationService.createTopic(request));
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ResponseEntity<List<MemoTopic>> getTopicsByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(configurationService.getTopicsByCategory(categoryId));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<MemoTopic> getTopic(@PathVariable java.util.UUID topicId) {
        return ResponseEntity.ok(configurationService.getTopic(topicId));
    }

    @PutMapping("/topics/{topicId}/workflow")
    public ResponseEntity<MemoTopic> updateTopicWorkflow(
            @PathVariable java.util.UUID topicId,
            @RequestBody String workflowXml) {
        return ResponseEntity.ok(configurationService.updateTopicWorkflow(topicId, workflowXml));
    }

    @PutMapping("/topics/{topicId}/form-schema")
    public ResponseEntity<MemoTopic> updateTopicFormSchema(
            @PathVariable java.util.UUID topicId,
            @RequestBody java.util.Map<String, Object> formSchema) {
        return ResponseEntity.ok(configurationService.updateTopicFormSchema(topicId, formSchema));
    }

    @PutMapping("/topics/{topicId}/workflow-template")
    public ResponseEntity<MemoTopic> updateTopicWorkflowTemplate(
            @PathVariable java.util.UUID topicId,
            @RequestBody String workflowTemplateId) {
        return ResponseEntity.ok(configurationService.updateTopicWorkflowTemplate(topicId, workflowTemplateId));
    }

    /**
     * Deploy workflow BPMN to Flowable engine.
     * This creates a deployable process definition and links the templateId back to
     * the topic.
     */
    @PostMapping("/topics/{topicId}/deploy-workflow")
    public ResponseEntity<MemoTopic> deployTopicWorkflow(
            @PathVariable java.util.UUID topicId,
            @RequestHeader(value = "X-Product-Id") String productId) {
        return ResponseEntity.ok(configurationService.deployTopicWorkflow(topicId, productId));
    }

    /**
     * Update memo-wide viewer configuration.
     * Allows setting users, roles, or departments that can view all memos for this
     * topic.
     */
    @PatchMapping("/topics/{topicId}/viewers")
    public ResponseEntity<MemoTopic> updateTopicViewers(
            @PathVariable java.util.UUID topicId,
            @RequestBody java.util.Map<String, Object> viewerConfig) {
        return ResponseEntity.ok(configurationService.updateTopicViewers(topicId, viewerConfig));
    }

    /**
     * Update override permissions for a topic.
     * Controls what users can customize when creating memos for this topic.
     */
    @PatchMapping("/topics/{topicId}/override-permissions")
    public ResponseEntity<MemoTopic> updateTopicOverridePermissions(
            @PathVariable java.util.UUID topicId,
            @RequestBody java.util.Map<String, Object> overridePermissions) {
        return ResponseEntity.ok(configurationService.updateTopicOverridePermissions(topicId, overridePermissions));
    }

    /**
     * Get available workflow variables for condition building.
     * Returns variables from memo fields, form schema, and initiator context.
     */
    @GetMapping("/topics/{topicId}/workflow-variables")
    public ResponseEntity<List<com.enterprise.memo.dto.WorkflowVariable>> getWorkflowVariables(
            @PathVariable java.util.UUID topicId) {
        return ResponseEntity.ok(configurationService.getWorkflowVariables(topicId));
    }

    /**
     * Copy deployed workflow to create a new editable version.
     * This snapshots the current deployed version to history, then
     * increments the version number and clears the workflow template ID,
     * allowing the admin to modify and redeploy the workflow.
     * 
     * Existing memos continue using the old deployed version via the snapshot.
     */
    @PostMapping("/topics/{topicId}/copy-workflow")
    public ResponseEntity<MemoTopic> copyTopicWorkflow(@PathVariable java.util.UUID topicId) {
        return ResponseEntity.ok(configurationService.copyTopicWorkflow(topicId));
    }

    /**
     * Get workflow version history for a topic.
     * Returns all deployed versions with their process template IDs and config
     * snapshots.
     */
    @GetMapping("/topics/{topicId}/versions")
    public ResponseEntity<java.util.List<com.enterprise.memo.entity.WorkflowVersionHistory>> getWorkflowVersions(
            @PathVariable java.util.UUID topicId) {
        return ResponseEntity.ok(configurationService.getWorkflowVersions(topicId));
    }

    /**
     * Get a specific workflow version.
     */
    @GetMapping("/topics/{topicId}/versions/{version}")
    public ResponseEntity<com.enterprise.memo.entity.WorkflowVersionHistory> getWorkflowVersion(
            @PathVariable java.util.UUID topicId,
            @PathVariable int version) {
        return ResponseEntity.ok(configurationService.getWorkflowVersion(topicId, version));
    }
}
