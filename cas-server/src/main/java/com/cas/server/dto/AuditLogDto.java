package com.cas.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private UUID id;
    private String eventType;
    private String actorType;
    private UUID actorId;
    private String targetType;
    private UUID targetId;
    private String productCode;
    private String ipAddress;
    private Map<String, Object> details;
    private Instant createdAt;
}
