package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.TaskConfigurationDTO;
import com.enterprise.workflow.service.TaskConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing task configurations.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/task-config")
@RequiredArgsConstructor
public class TaskConfigurationController {

    private final TaskConfigurationService taskConfigService;

    /**
     * Get all task configurations for a template.
     */
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<TaskConfigurationDTO>> getByTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(taskConfigService.getTaskConfigs(templateId));
    }

    /**
     * Get task configuration by template and task key.
     */
    @GetMapping("/template/{templateId}/task/{taskKey}")
    public ResponseEntity<TaskConfigurationDTO> getByTaskKey(
            @PathVariable UUID templateId,
            @PathVariable String taskKey) {
        try {
            return ResponseEntity.ok(taskConfigService.getTaskConfig(templateId, taskKey));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create or update a task configuration.
     */
    @PostMapping("/template/{templateId}")
    public ResponseEntity<TaskConfigurationDTO> save(
            @PathVariable UUID templateId,
            @RequestBody TaskConfigurationDTO dto) {
        try {
            return ResponseEntity.ok(taskConfigService.saveTaskConfig(templateId, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bulk save task configurations for a template.
     */
    @PostMapping("/template/{templateId}/bulk")
    public ResponseEntity<List<TaskConfigurationDTO>> saveBulk(
            @PathVariable UUID templateId,
            @RequestBody List<TaskConfigurationDTO> configs) {
        try {
            return ResponseEntity.ok(taskConfigService.saveAllTaskConfigs(templateId, configs));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a task configuration.
     */
    @DeleteMapping("/{configId}")
    public ResponseEntity<Void> delete(@PathVariable UUID configId) {
        taskConfigService.deleteTaskConfig(configId);
        return ResponseEntity.noContent().build();
    }
}
