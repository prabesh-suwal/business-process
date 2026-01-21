package com.enterprise.policyengine.controller;

import com.enterprise.policyengine.dto.PolicyRequest;
import com.enterprise.policyengine.dto.PolicyResponse;
import com.enterprise.policyengine.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for policy management (CRUD).
 * Used by Super Admin to manage policies.
 */
@Slf4j
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "CRUD operations for policies")
public class PolicyController {

    private final PolicyService policyService;

    /**
     * List all policies.
     */
    @GetMapping
    @Operation(summary = "List all policies", description = "Get all active policies with their rules")
    public ResponseEntity<List<PolicyResponse>> listAll() {
        return ResponseEntity.ok(policyService.findAll());
    }

    /**
     * Get a policy by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID", description = "Get a single policy with all its rules")
    public ResponseEntity<PolicyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.findById(id));
    }

    /**
     * Get a policy by name.
     */
    @GetMapping("/name/{name}")
    @Operation(summary = "Get policy by name", description = "Get a policy by its unique name")
    public ResponseEntity<PolicyResponse> getByName(@PathVariable String name) {
        return ResponseEntity.ok(policyService.findByName(name));
    }

    /**
     * Create a new policy.
     */
    @PostMapping
    @Operation(summary = "Create policy", description = "Create a new policy with rules")
    public ResponseEntity<PolicyResponse> create(
            @Valid @RequestBody PolicyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        PolicyResponse response = policyService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing policy.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update policy", description = "Update an existing policy and its rules")
    public ResponseEntity<PolicyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PolicyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        PolicyResponse response = policyService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a policy.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete policy", description = "Delete a policy and all its rules")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate a policy.
     */
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate policy", description = "Enable a policy for evaluation")
    public ResponseEntity<PolicyResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.activate(id));
    }

    /**
     * Deactivate a policy.
     */
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate policy", description = "Disable a policy from evaluation")
    public ResponseEntity<PolicyResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.deactivate(id));
    }
}
