package com.enterprise.makerchecker.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConfigResponse {
    private UUID id;
    private UUID productId;
    private String serviceName;
    private String endpointPattern;
    private String httpMethod;
    private String endpointGroup;
    private String description;
    private boolean sameMakerCanCheck;
    private boolean enabled;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // SLA
    private Integer deadlineHours;
    private String escalationRole;
    private Boolean autoExpire;

    // Checker users
    private List<UUID> checkerUserIds;
}
