package com.enterprise.workflow.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for process instance management.
 * Used for starting, querying, and managing running processes.
 */
@Tag(name = "Process Instances", description = "Endpoints for managing BPMN process instances")
@RestController
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessRuntimeService processRuntimeService;

    /**
     * Start a new process instance.
     */
    @Operation(summary = "Start Process", description = "Starts a new process instance")
    @PostMapping
    @com.cas.common.dto.ApiMessage("Process started successfully")
    public ResponseEntity<ProcessInstanceDTO> startProcess(@Valid @RequestBody StartProcessRequest request) {
        UserContext user = UserContextHolder.getContext();
        UUID startedBy = user != null && user.getUserId() != null ? UUID.fromString(user.getUserId()) : null;
        String userName = user != null ? user.getName() : null;
        ProcessInstanceDTO instance = processRuntimeService.startProcess(request, startedBy, userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(instance);
    }

    /**
     * Get a process instance by Flowable ID.
     */
    @Operation(summary = "Get Process Instance", description = "Retrieves a process instance by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstance(@PathVariable String id) {
        return ResponseEntity.ok(processRuntimeService.getProcessInstance(id));
    }

    /**
     * Get process instances by product.
     */
    @Operation(summary = "Get Processes by Product", description = "Retrieves process instances for a specific product")
    @GetMapping
    public ResponseEntity<Page<ProcessInstanceDTO>> getProcessInstances(
            @RequestParam UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(processRuntimeService.getProcessInstancesByProduct(productId, pageable));
    }

    /**
     * Get process instances started by current user.
     */
    @Operation(summary = "Get My Processes", description = "Retrieves process instances started by the current user")
    @GetMapping("/my")
    public ResponseEntity<Page<ProcessInstanceDTO>> getMyProcessInstances(
            @PageableDefault(size = 20) Pageable pageable) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity
                .ok(processRuntimeService.getProcessInstancesByUser(UUID.fromString(user.getUserId()), pageable));
    }

    /**
     * Get process variables.
     */
    @Operation(summary = "Get Process Variables", description = "Retrieves variables for a process instance")
    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<String, Object>> getProcessVariables(@PathVariable String id) {
        return ResponseEntity.ok(processRuntimeService.getProcessVariables(id));
    }

    /**
     * Set a process variable.
     */
    @Operation(summary = "Set Process Variable", description = "Sets or updates a variable for a process instance")
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
    @Operation(summary = "Cancel Process", description = "Cancels a running process instance")
    @DeleteMapping("/{id}")
    @com.cas.common.dto.ApiMessage("Process cancelled successfully")
    public ResponseEntity<Void> cancelProcess(
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        UserContext user = UserContextHolder.getContext();
        UUID cancelledBy = user != null && user.getUserId() != null ? UUID.fromString(user.getUserId()) : null;
        String userName = user != null ? user.getName() : null;
        processRuntimeService.cancelProcess(id, cancelledBy, userName, reason);
        return ResponseEntity.noContent().build();
    }
}
