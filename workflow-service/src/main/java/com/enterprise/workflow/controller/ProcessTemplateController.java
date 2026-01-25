package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.service.ProcessDesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for process template management.
 * Used by Admin UI for designing and deploying workflows.
 */
@RestController
@RequestMapping("/api/process-templates")
@RequiredArgsConstructor
public class ProcessTemplateController {

    private final ProcessDesignService processDesignService;

    /**
     * Create a new process template (DRAFT status).
     */
    @PostMapping
    public ResponseEntity<ProcessTemplateDTO> createTemplate(
            @Valid @RequestBody CreateProcessTemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        ProcessTemplateDTO template = processDesignService.createTemplate(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    /**
     * Get a process template by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessTemplateDTO> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(processDesignService.getTemplate(id));
    }

    /**
     * Get all process templates, optionally filtered by product.
     */
    @GetMapping
    public ResponseEntity<List<ProcessTemplateDTO>> getTemplates(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<ProcessTemplateDTO> templates;
        if (productId != null) {
            templates = activeOnly
                    ? processDesignService.getActiveTemplatesByProduct(productId)
                    : processDesignService.getTemplatesByProduct(productId);
        } else {
            // Return all templates (optionally filtered by active status) when no productId
            // specified
            templates = activeOnly
                    ? processDesignService.getAllActiveTemplates()
                    : processDesignService.getAllTemplates();
        }

        return ResponseEntity.ok(templates);
    }

    /**
     * Update a process template (DRAFT only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProcessTemplateDTO> updateTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateProcessTemplateRequest request) {

        return ResponseEntity.ok(processDesignService.updateTemplate(id, request));
    }

    /**
     * Deploy a process template to Flowable (makes it ACTIVE).
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<ProcessTemplateDTO> deployTemplate(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID deployedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(processDesignService.deployTemplate(id, deployedBy));
    }

    /**
     * Deploy raw BPMN XML directly to Flowable.
     * Used by memo-service to deploy topic workflows without going through template
     * management.
     * Returns the Flowable process definition ID.
     */
    @PostMapping("/deploy-bpmn")
    public ResponseEntity<java.util.Map<String, Object>> deployBpmn(
            @RequestBody java.util.Map<String, String> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String processKey = request.get("processKey");
        String processName = request.get("processName");
        String bpmnXml = request.get("bpmnXml");

        if (processKey == null || bpmnXml == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "processKey and bpmnXml are required"));
        }

        String processDefinitionId = processDesignService.deployRawBpmn(processKey, processName, bpmnXml);

        return ResponseEntity.ok(java.util.Map.of(
                "processDefinitionId", processDefinitionId,
                "message", "Workflow deployed successfully"));
    }

    /**
     * Deprecate an ACTIVE process template.
     */
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<ProcessTemplateDTO> deprecateTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(processDesignService.deprecateTemplate(id));
    }

    /**
     * Delete a process template (DRAFT only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        processDesignService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Create a new version of an existing process template.
     * This creates a new DRAFT template based on the existing template's BPMN.
     */
    @PostMapping("/{id}/new-version")
    public ResponseEntity<ProcessTemplateDTO> createNewVersion(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(processDesignService.createNewVersion(id, createdBy));
    }

    // ==================== Form Management Endpoints ====================

    /**
     * Attach a form to a task in the process template.
     */
    @PostMapping("/{id}/forms")
    public ResponseEntity<ProcessTemplateFormDTO> attachForm(
            @PathVariable UUID id,
            @Valid @RequestBody AttachFormRequest request) {

        ProcessTemplateFormDTO mapping = processDesignService.mapFormToTask(
                id,
                request.getTaskKey(),
                request.getFormDefinitionId(),
                request.getFormType());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
    }

    /**
     * Get all form mappings for a process template.
     */
    @GetMapping("/{id}/forms")
    public ResponseEntity<List<ProcessTemplateFormDTO>> getFormsForTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(processDesignService.getFormsForTemplate(id));
    }

    /**
     * Get the form mapped to a specific task in a process template.
     */
    @GetMapping("/{id}/tasks/{taskKey}/form")
    public ResponseEntity<ProcessTemplateFormDTO> getFormForTask(
            @PathVariable UUID id,
            @PathVariable String taskKey) {

        ProcessTemplateFormDTO form = processDesignService.getFormForTask(id, taskKey);
        if (form == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(form);
    }

    /**
     * Remove form mapping from a task.
     */
    @DeleteMapping("/{id}/tasks/{taskKey}/form")
    public ResponseEntity<Void> detachForm(
            @PathVariable UUID id,
            @PathVariable String taskKey) {

        processDesignService.detachForm(id, taskKey);
        return ResponseEntity.noContent().build();
    }
}
