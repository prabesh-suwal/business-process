package com.enterprise.audit.controller;

import com.cas.common.audit.AuditEvent;
import com.enterprise.audit.dto.AuditLogResponse;
import com.enterprise.audit.dto.AuditSearchRequest;
import com.enterprise.audit.dto.IntegrityCheckResult;
import com.enterprise.audit.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-events")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * Receive and persist an audit event from other services.
     * This is the main endpoint that HttpAuditEventPublisher posts to.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditLogResponse createAuditEvent(@Valid @RequestBody AuditEvent event) {
        log.debug("Received audit event: action={}, resource={}/{}",
                event.getAction(), event.getResourceType(), event.getResourceId());
        return auditLogService.createAuditLog(event);
    }

    /**
     * Get a specific audit log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLog(@PathVariable UUID id) {
        return auditLogService.getAuditLog(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search audit logs with various filters
     */
    @PostMapping("/search")
    public Page<AuditLogResponse> searchAuditLogs(
            @RequestBody AuditSearchRequest request,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return auditLogService.searchAuditLogs(request, pageable);
    }

    /**
     * Get all audit logs for a correlation ID (trace a transaction across services)
     */
    @GetMapping("/correlation/{correlationId}")
    public List<AuditLogResponse> getByCorrelationId(@PathVariable String correlationId) {
        return auditLogService.getByCorrelationId(correlationId);
    }

    /**
     * Get audit trail for a specific resource
     */
    @GetMapping("/resource/{resourceType}/{resourceId}")
    public Page<AuditLogResponse> getResourceAuditTrail(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return auditLogService.getResourceAuditTrail(resourceType, resourceId, pageable);
    }

    /**
     * Get audit trail for a specific actor (user)
     */
    @GetMapping("/actor/{actorId}")
    public Page<AuditLogResponse> getActorAuditTrail(
            @PathVariable String actorId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return auditLogService.getActorAuditTrail(actorId, pageable);
    }

    /**
     * Verify integrity of audit log chain (admin only)
     */
    @GetMapping("/verify-integrity")
    public IntegrityCheckResult verifyIntegrity(
            @RequestParam Long startSequence,
            @RequestParam Long endSequence) {
        return auditLogService.verifyIntegrity(startSequence, endSequence);
    }
}
