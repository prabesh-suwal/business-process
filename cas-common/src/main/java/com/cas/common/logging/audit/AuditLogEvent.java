package com.cas.common.logging.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit Log event for compliance/legal logging.
 * Records who changed what, before/after values, for regulatory compliance.
 * 
 * Retention: 5-10 years (immutable, tamper-proof)
 * Audience: Internal audit, legal, fraud investigation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEvent {

    @Builder.Default
    private String auditId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String correlationId;
    private String serviceName;

    // Event Classification
    private AuditEventType eventType;
    private String moduleName;

    // Actor Information (Who performed the action)
    private String performedByUserId;
    private String performedByUsername;
    private String performedByRole;
    private String performedByDepartment;
    private String performedByBranch;
    private String ipAddress;
    private String deviceId;
    private String sessionId;

    // Entity Information (What was affected)
    private String entityName;
    private String entityId;
    private String parentEntityId;
    private String businessKey;

    // Change Details
    private String action;
    private Object oldValue; // Will be serialized to JSON
    private Object newValue; // Will be serialized to JSON
    private List<String> changedFields;
    private String remarks;
    private Integer approvalLevel;

    // Result
    @Builder.Default
    private String result = "SUCCESS";
    private String errorMessage;

    // Integrity (populated by audit-service)
    private String previousHash;
    private String recordHash;
}
