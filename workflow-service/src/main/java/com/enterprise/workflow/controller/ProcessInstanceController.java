package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.service.ProcessRuntimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for process instance management.
 * Used for starting, querying, and managing running processes.
 */
@RestController
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessRuntimeService processRuntimeService;

    /**
     * Start a new process instance.
     */
    @PostMapping
    public ResponseEntity<ProcessInstanceDTO> startProcess(
            @Valid @RequestBody StartProcessRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        UUID startedBy = userId != null ? UUID.fromString(userId) : null;
        ProcessInstanceDTO instance = processRuntimeService.startProcess(request, startedBy, userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(instance);
    }

    /**
     * Get a process instance by Flowable ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstance(@PathVariable String id) {
        return ResponseEntity.ok(processRuntimeService.getProcessInstance(id));
    }

    /**
     * Get process instances by product.
     */
    @GetMapping
    public ResponseEntity<Page<ProcessInstanceDTO>> getProcessInstances(
            @RequestParam UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(processRuntimeService.getProcessInstancesByProduct(productId, pageable));
    }

    /**
     * Get process instances started by current user.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ProcessInstanceDTO>> getMyProcessInstances(
            @RequestHeader(value = "X-User-Id") String userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(processRuntimeService.getProcessInstancesByUser(UUID.fromString(userId), pageable));
    }

    /**
     * Get process variables.
     */
    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<String, Object>> getProcessVariables(@PathVariable String id) {
        return ResponseEntity.ok(processRuntimeService.getProcessVariables(id));
    }

    /**
     * Set a process variable.
     */
    @PutMapping("/{id}/variables/{variableName}")
    public ResponseEntity<Void> setProcessVariable(
            @PathVariable String id,
            @PathVariable String variableName,
            @RequestBody Object value) {

        processRuntimeService.setProcessVariable(id, variableName, value);
        return ResponseEntity.ok().build();
    }

    /**
     * Cancel a process instance.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelProcess(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        UUID cancelledBy = userId != null ? UUID.fromString(userId) : null;
        processRuntimeService.cancelProcess(id, cancelledBy, userName, reason);
        return ResponseEntity.noContent().build();
    }
}
