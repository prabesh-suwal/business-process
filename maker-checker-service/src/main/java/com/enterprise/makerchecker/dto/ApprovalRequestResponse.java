package com.enterprise.makerchecker.dto;

import com.enterprise.makerchecker.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ApprovalRequestResponse {
    private UUID id;
    private UUID configId;
    private ApprovalStatus status;

    // Original request
    private String httpMethod;
    private String requestPath;
    private String requestBody;
    private Map<String, String> requestHeaders;
    private String queryParams;

    // Maker
    private String makerUserId;
    private String makerUserName;
    private String makerRoles;
    private String makerProductCode;

    // Checker
    private String checkerUserId;
    private String checkerUserName;
    private String checkerComment;

    // Execution result
    private Integer responseStatus;
    private String responseBody;

    // Config info
    private String serviceName;
    private String endpointPattern;
    private String configDescription;

    // Timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime resolvedAt;
}
