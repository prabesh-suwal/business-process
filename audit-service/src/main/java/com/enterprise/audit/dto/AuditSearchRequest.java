package com.enterprise.audit.dto;

import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for searching audit logs with various filters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSearchRequest {
    private String actorId;
    private AuditCategory category;
    private AuditAction action;
    private String resourceType;
    private String resourceId;
    private String serviceName;
    private String productCode;
    private String correlationId;
    private Instant startTime;
    private Instant endTime;
}
