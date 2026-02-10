package com.enterprise.audit.controller;

import com.cas.common.audit.AuditEvent;
import com.cas.common.logging.audit.AuditLogEvent;
import com.enterprise.audit.dto.AuditLogResponse;
import com.enterprise.audit.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for enhanced audit logs (compliance).
 * Provides the new /api/logs/audit endpoint for the three-tier logging system.
 * Also accepts the new AuditLogEvent format from AuditLogger.
 */
@RestController
@RequestMapping("/api/logs/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Ingest a new audit log event from the builder-based AuditLogger.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> ingestEnhancedAudit(@Valid @RequestBody AuditLogEvent event) {
        log.debug("Ingesting enhanced audit event: {} {} on {}/{}",
                event.getEventType(), event.getAction(), event.getEntityName(), event.getEntityId());

        // Convert AuditLogEvent to AuditEvent for compatibility with existing service
        AuditEvent legacyEvent = convertToLegacyEvent(event);
        auditLogService.createAuditLog(legacyEvent);

        return ResponseEntity.ok().build();
    }

    private AuditEvent convertToLegacyEvent(AuditLogEvent event) {
        return AuditEvent.builder()
                .correlationId(event.getCorrelationId())
                .timestamp(event.getTimestamp())
                .serviceName(event.getServiceName())
                .actorId(event.getPerformedByUserId())
                .actorName(event.getPerformedByUsername())
                .actorType(null) // Will be inferred
                .actorRoles(event.getPerformedByRole() != null ? java.util.List.of(event.getPerformedByRole()) : null)
                .ipAddress(event.getIpAddress())
                .action(event.getEventType() != null
                        ? com.cas.common.audit.AuditAction.valueOf(event.getEventType().name())
                        : null)
                .category(com.cas.common.audit.AuditCategory.DATA_ACCESS) // Default
                .resourceType(event.getEntityName())
                .resourceId(event.getEntityId())
                .description(event.getAction() + (event.getRemarks() != null ? ": " + event.getRemarks() : ""))
                .result("SUCCESS".equals(event.getResult()) ? com.cas.common.audit.AuditResult.SUCCESS
                        : com.cas.common.audit.AuditResult.FAILURE)
                .failureReason(event.getErrorMessage())
                .metadata(buildMetadata(event))
                .build();
    }

    private java.util.Map<String, Object> buildMetadata(AuditLogEvent event) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        if (event.getOldValue() != null)
            metadata.put("oldValue", event.getOldValue());
        if (event.getNewValue() != null)
            metadata.put("newValue", event.getNewValue());
        if (event.getChangedFields() != null)
            metadata.put("changedFields", event.getChangedFields());
        if (event.getBusinessKey() != null)
            metadata.put("businessKey", event.getBusinessKey());
        if (event.getModuleName() != null)
            metadata.put("moduleName", event.getModuleName());
        if (event.getPerformedByDepartment() != null)
            metadata.put("department", event.getPerformedByDepartment());
        if (event.getPerformedByBranch() != null)
            metadata.put("branch", event.getPerformedByBranch());
        if (event.getDeviceId() != null)
            metadata.put("deviceId", event.getDeviceId());
        if (event.getSessionId() != null)
            metadata.put("sessionId", event.getSessionId());
        if (event.getApprovalLevel() != null)
            metadata.put("approvalLevel", event.getApprovalLevel());
        return metadata.isEmpty() ? null : metadata;
    }
}
