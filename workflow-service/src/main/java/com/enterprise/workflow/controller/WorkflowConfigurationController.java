package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.CreateWorkflowConfigRequest;
import com.enterprise.workflow.dto.ResolvedAssignment;
import com.enterprise.workflow.dto.WorkflowConfigurationDTO;
import com.enterprise.workflow.service.AssignmentRuleEvaluator;
import com.enterprise.workflow.service.WorkflowConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow-configs")
@RequiredArgsConstructor
public class WorkflowConfigurationController {

    private final WorkflowConfigurationService configService;
    private final AssignmentRuleEvaluator assignmentEvaluator;

    @PostMapping
    public ResponseEntity<WorkflowConfigurationDTO> createConfiguration(
            @Valid @RequestBody CreateWorkflowConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createConfiguration(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowConfigurationDTO>> getConfigurations(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<WorkflowConfigurationDTO> configs;
        if (productCode != null) {
            configs = activeOnly
                    ? configService.getActiveByProductCode(productCode)
                    : configService.getByProductCode(productCode);
        } else {
            configs = activeOnly
                    ? configService.getAllActive()
                    : configService.getAll();
        }
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowConfigurationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(configService.getById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<WorkflowConfigurationDTO> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(configService.getByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowConfigurationDTO> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkflowConfigRequest request) {
        return ResponseEntity.ok(configService.updateConfiguration(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        configService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        configService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    // Task-Form Mapping Endpoints

    @GetMapping("/code/{code}/tasks/{taskKey}/form")
    public ResponseEntity<Map<String, Object>> getFormForTask(
            @PathVariable String code,
            @PathVariable String taskKey) {
        UUID formId = configService.getFormForTask(code, taskKey);
        return ResponseEntity.ok(Map.of(
                "configCode", code,
                "taskKey", taskKey,
                "formId", formId != null ? formId.toString() : ""));
    }

    @PutMapping("/{id}/tasks/{taskKey}/form")
    public ResponseEntity<WorkflowConfigurationDTO> setTaskFormMapping(
            @PathVariable UUID id,
            @PathVariable String taskKey,
            @RequestBody Map<String, UUID> body) {
        UUID formId = body.get("formId");
        return ResponseEntity.ok(configService.setTaskFormMapping(id, taskKey, formId));
    }

    @DeleteMapping("/{id}/tasks/{taskKey}/form")
    public ResponseEntity<WorkflowConfigurationDTO> removeTaskFormMapping(
            @PathVariable UUID id,
            @PathVariable String taskKey) {
        return ResponseEntity.ok(configService.removeTaskFormMapping(id, taskKey));
    }

    // Assignment Rule Endpoints

    @GetMapping("/code/{code}/tasks/{taskKey}/assignment")
    public ResponseEntity<Map<String, Object>> getAssignmentRuleForTask(
            @PathVariable String code,
            @PathVariable String taskKey) {
        Map<String, Object> rule = configService.getAssignmentRuleForTask(code, taskKey);
        return ResponseEntity.ok(Map.of(
                "configCode", code,
                "taskKey", taskKey,
                "assignmentRule", rule != null ? rule : Map.of()));
    }

    /**
     * Resolve assignment for a task based on process variables.
     * This evaluates conditional rules to determine who should be assigned.
     */
    @PostMapping("/code/{code}/tasks/{taskKey}/resolve-assignment")
    public ResponseEntity<ResolvedAssignment> resolveAssignment(
            @PathVariable String code,
            @PathVariable String taskKey,
            @RequestBody Map<String, Object> processVariables) {
        ResolvedAssignment resolved = assignmentEvaluator.resolveAssignment(code, taskKey, processVariables);
        return ResponseEntity.ok(resolved);
    }
}
