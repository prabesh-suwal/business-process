package com.enterprise.form.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.form.dto.*;
import com.enterprise.form.service.FormBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for form definition management.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormDefinitionController {

    private final FormBuilderService formBuilderService;

    /**
     * Create a new form definition.
     */
    @PostMapping
    public ResponseEntity<FormDefinitionDTO> createForm(@Valid @RequestBody CreateFormRequest request) {
        UserContext user = UserContextHolder.getContext();
        UUID createdBy = user != null && user.getUserId() != null ? UUID.fromString(user.getUserId()) : null;
        FormDefinitionDTO form = formBuilderService.createForm(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(form);
    }

    /**
     * Get a form definition by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> getForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formBuilderService.getForm(id));
    }

    /**
     * Get forms by product.
     */
    @GetMapping
    public ResponseEntity<List<FormDefinitionDTO>> getForms(
            @RequestParam UUID productId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<FormDefinitionDTO> forms = activeOnly
                ? formBuilderService.getActiveFormsByProduct(productId)
                : formBuilderService.getFormsByProduct(productId);

        return ResponseEntity.ok(forms);
    }

    /**
     * Update a form definition.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> updateForm(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFormRequest request) {

        return ResponseEntity.ok(formBuilderService.updateForm(id, request));
    }

    /**
     * Activate a form.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<FormDefinitionDTO> activateForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formBuilderService.activateForm(id));
    }

    /**
     * Deprecate a form.
     */
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<FormDefinitionDTO> deprecateForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formBuilderService.deprecateForm(id));
    }

    /**
     * Delete a form (DRAFT only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable UUID id) {
        formBuilderService.deleteForm(id);
        return ResponseEntity.noContent().build();
    }
}
