package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.ActionTypeDefinition;
import com.enterprise.workflow.dto.TaskConfigurationDTO;
import com.enterprise.workflow.service.TaskConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for task outcome configuration.
 * Provides runtime access to the outcome options configured for a task
 * in the workflow designer (variable name, option values/labels/styles).
 *
 * Also serves the predefined action type catalog that admins select from
 * when configuring task outcomes.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskOutcomeController {

    private final org.flowable.engine.TaskService taskService;
    private final RuntimeService runtimeService;
    private final TaskConfigurationService taskConfigService;

    /**
     * Predefined action types that admins can select from.
     * These represent system capabilities with pre-wired behavior.
     */
    private static final List<ActionTypeDefinition> PREDEFINED_ACTION_TYPES = List.of(
            ActionTypeDefinition.builder()
                    .actionType("APPROVE")
                    .defaultLabel("Approve")
                    .description("Approves the item and moves it to the next step in the workflow")
                    .defaultStyle("success")
                    .defaultSets(Map.of("decision", "APPROVED"))
                    .requiresComment(false)
                    .requiresTargetStep(false)
                    .build(),
            ActionTypeDefinition.builder()
                    .actionType("REJECT")
                    .defaultLabel("Reject")
                    .description("Rejects the item and ends the workflow or routes to rejection handling")
                    .defaultStyle("danger")
                    .defaultSets(Map.of("decision", "REJECTED"))
                    .requiresComment(true)
                    .requiresTargetStep(false)
                    .build(),
            ActionTypeDefinition.builder()
                    .actionType("ESCALATE")
                    .defaultLabel("Escalate")
                    .description("Escalates the item to a higher authority for review")
                    .defaultStyle("info")
                    .defaultSets(Map.of("decision", "ESCALATED"))
                    .requiresComment(false)
                    .requiresTargetStep(false)
                    .build(),
            ActionTypeDefinition.builder()
                    .actionType("BACK_TO_INITIATOR")
                    .defaultLabel("Return to Initiator")
                    .description("Sends the item back to the original initiator for correction")
                    .defaultStyle("warning")
                    .defaultSets(Map.of("decision", "RETURNED_TO_INITIATOR"))
                    .requiresComment(true)
                    .requiresTargetStep(false)
                    .build(),
            ActionTypeDefinition.builder()
                    .actionType("SEND_BACK")
                    .defaultLabel("Send Back")
                    .description("Sends the item back to the previous step it came from (based on execution history)")
                    .defaultStyle("warning")
                    .defaultSets(Map.of("decision", "SENT_BACK"))
                    .requiresComment(true)
                    .requiresTargetStep(false) // Auto-determined from history
                    .build(),

            ActionTypeDefinition.builder()
                    .actionType("BACK_TO_STEP")
                    .defaultLabel("Return to Step")
                    .description("Sends the item back to a specific previous step chosen by the user")
                    .defaultStyle("warning")
                    .defaultSets(Map.of("decision", "SENT_BACK"))
                    .requiresComment(false)
                    .requiresTargetStep(true)
                    .build(),
            ActionTypeDefinition.builder()
                    .actionType("DELEGATE")
                    .defaultLabel("Delegate")
                    .description("Reassigns the task to another user or group without changing the workflow flow")
                    .defaultStyle("info")
                    .defaultSets(Map.of())
                    .requiresComment(false)
                    .requiresTargetStep(false)
                    .build());

    /**
     * Get the predefined action types that admins can select from
     * when configuring task outcomes.
     *
     * Returns all available action types with their defaults.
     */
    @GetMapping("/action-types")
    public ResponseEntity<List<ActionTypeDefinition>> getActionTypes() {
        return ResponseEntity.ok(PREDEFINED_ACTION_TYPES);
    }

    /**
     * Get the outcome configuration for a running task.
     * Returns the options defined in the workflow designer, each with a sets map.
     *
     * Response format (enterprise routing pattern):
     * {
     * "options": [
     * { "label": "Approve", "style": "success", "sets": { "nextStage": "APPROVED" }
     * },
     * { "label": "Reject", "style": "danger", "sets": { "nextStage": "REJECTED" } }
     * ]
     * }
     *
     * BPMN gateways should use: ${nextStage == "APPROVED"}
     *
     * Returns empty body (204) if no outcome config is defined for this task.
     */
    @GetMapping("/{taskId}/outcome-config")
    public ResponseEntity<Map<String, Object>> getOutcomeConfig(@PathVariable String taskId) {
        log.debug("Getting outcome config for task: {}", taskId);

        // 1. Find the Flowable task
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return ResponseEntity.notFound().build();
        }

        // 2. Get processTemplateId from process variables
        String processInstanceId = task.getProcessInstanceId();
        Object templateIdVar = runtimeService.getVariable(processInstanceId, "processTemplateId");

        if (templateIdVar == null) {
            log.debug("No processTemplateId variable found for process {}", processInstanceId);
            return ResponseEntity.noContent().build();
        }

        UUID processTemplateId;
        try {
            processTemplateId = templateIdVar instanceof UUID
                    ? (UUID) templateIdVar
                    : UUID.fromString(templateIdVar.toString());
        } catch (Exception e) {
            log.warn("Invalid processTemplateId format: {}", templateIdVar);
            return ResponseEntity.noContent().build();
        }

        // 3. Look up task configuration
        String taskKey = task.getTaskDefinitionKey();
        TaskConfigurationDTO taskConfig;
        try {
            taskConfig = taskConfigService.getTaskConfig(processTemplateId, taskKey);
        } catch (IllegalArgumentException e) {
            log.debug("No task config for template {} key {}", processTemplateId, taskKey);
            return ResponseEntity.noContent().build();
        }

        // 4. Extract outcomeConfig from the dedicated field
        Map<String, Object> outcomeConfig = taskConfig.getOutcomeConfig();
        if (outcomeConfig == null || outcomeConfig.isEmpty()) {
            // Fallback: check the generic config map for backward compatibility
            Map<String, Object> genericConfig = taskConfig.getConfig();
            if (genericConfig != null && genericConfig.containsKey("outcomeConfig")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> legacyOutcomeConfig = (Map<String, Object>) genericConfig.get("outcomeConfig");
                outcomeConfig = legacyOutcomeConfig;
            }
        }

        if (outcomeConfig == null || outcomeConfig.isEmpty()) {
            log.debug("No outcomeConfig in task config for {}", taskKey);
            return ResponseEntity.noContent().build();
        }

        log.info("Returning outcome config for task {} (key={}): {} options",
                taskId, taskKey,
                outcomeConfig.get("options") != null ? ((java.util.List<?>) outcomeConfig.get("options")).size() : 0);

        return ResponseEntity.ok(outcomeConfig);
    }
}
