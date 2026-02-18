package com.enterprise.workflow.controller;

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

        // 4. Extract outcomeConfig from the config JSON
        Map<String, Object> config = taskConfig.getConfig();
        if (config == null || !config.containsKey("outcomeConfig")) {
            log.debug("No outcomeConfig in task config for {}", taskKey);
            return ResponseEntity.noContent().build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> outcomeConfig = (Map<String, Object>) config.get("outcomeConfig");

        log.info("Returning outcome config for task {} (key={}): {} options",
                taskId, taskKey,
                outcomeConfig.get("options") != null ? ((java.util.List<?>) outcomeConfig.get("options")).size() : 0);

        return ResponseEntity.ok(outcomeConfig);
    }
}
