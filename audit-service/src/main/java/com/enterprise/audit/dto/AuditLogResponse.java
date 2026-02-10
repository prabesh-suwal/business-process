package com.enterprise.audit.dto;

import com.cas.common.audit.ActorType;
import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import com.cas.common.audit.AuditResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for audit log entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private Long sequenceNumber;
    private Instant timestamp;

    // Actor
    private String actorId;
    private String actorName;
    private String actorEmail;
    private ActorType actorType;
    private List<String> actorRoles;
    private String ipAddress;

    // Action
    private AuditAction action;
    private AuditCategory category;

    // Resource
    private String resourceType;
    private String resourceId;

    // Context
    private String description;
    private String metadata;
    private AuditResult result;
    private String errorMessage;

    // Source
    private String serviceName;
    private String productCode;
    private String correlationId;

    // Integrity
    private String recordHash;
    private boolean integrityVerified;
}
