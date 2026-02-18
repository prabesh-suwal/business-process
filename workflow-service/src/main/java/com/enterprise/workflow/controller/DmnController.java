package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.CreateDecisionTableRequest;
import com.enterprise.workflow.dto.DecisionTableDTO;
import com.enterprise.workflow.dto.EvaluateDecisionRequest;
import com.enterprise.workflow.dto.EvaluateDecisionResponse;
import com.enterprise.workflow.service.DmnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for managing DMN Decision Tables.
 * Provides full lifecycle: create, edit, deploy, evaluate, version, deprecate.
 */
@RestController
@RequestMapping("/api/dmn")
@RequiredArgsConstructor
@Slf4j
public class DmnController {

    private final DmnService dmnService;

    /**
     * Create a new decision table (DRAFT status).
     */
    @PostMapping
    public ResponseEntity<DecisionTableDTO> create(
            @Valid @RequestBody CreateDecisionTableRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        String createdByName = userName != null ? userName : "System";

        DecisionTableDTO result = dmnService.create(request, createdBy, createdByName);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get a decision table by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DecisionTableDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(dmnService.get(id));
    }

    /**
     * List decision tables, optionally filtered by productId.
     */
    @GetMapping
    public ResponseEntity<List<DecisionTableDTO>> list(
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(dmnService.list(productId));
    }

    /**
     * List all active decision table keys (for BPMN Business Rule Task dropdown).
     */
    @GetMapping("/keys")
    public ResponseEntity<List<Map<String, String>>> listActiveKeys(
            @RequestParam(required = false) UUID productId) {

        List<DecisionTableDTO> active = dmnService.listActive(productId);
        List<Map<String, String>> keys = active.stream()
                .map(dt -> Map.of(
                        "key", dt.getKey(),
                        "name", dt.getName(),
                        "id", dt.getId().toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(keys);
    }

    /**
     * Update a DRAFT decision table (XML, name, description).
     */
    @PutMapping("/{id}")
    public ResponseEntity<DecisionTableDTO> update(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String dmnXml = body.get("dmnXml");
        String name = body.get("name");
        String description = body.get("description");

        return ResponseEntity.ok(dmnService.update(id, dmnXml, name, description));
    }

    /**
     * Deploy a decision table to the Flowable DMN engine.
     * Transitions status from DRAFT → ACTIVE.
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<DecisionTableDTO> deploy(@PathVariable UUID id) {
        return ResponseEntity.ok(dmnService.deploy(id));
    }

    /**
     * Delete a DRAFT decision table.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dmnService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deprecate an ACTIVE decision table.
     * Transitions status from ACTIVE → DEPRECATED.
     */
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<DecisionTableDTO> deprecate(@PathVariable UUID id) {
        return ResponseEntity.ok(dmnService.deprecate(id));
    }

    /**
     * Create a new version of an existing decision table.
     * Creates a DRAFT copy with incremented version number.
     */
    @PostMapping("/{id}/new-version")
    public ResponseEntity<DecisionTableDTO> createNewVersion(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        String createdByName = userName != null ? userName : "System";

        DecisionTableDTO result = dmnService.createNewVersion(id, createdBy, createdByName);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Test-evaluate a deployed decision table with sample variables.
     * Returns the outputs that would be produced at runtime.
     */
    @PostMapping("/evaluate/{key}")
    public ResponseEntity<EvaluateDecisionResponse> evaluate(
            @PathVariable String key,
            @RequestBody Map<String, Object> variables) {

        EvaluateDecisionResponse response = dmnService.evaluate(key, variables);
        return ResponseEntity.ok(response);
    }
}
