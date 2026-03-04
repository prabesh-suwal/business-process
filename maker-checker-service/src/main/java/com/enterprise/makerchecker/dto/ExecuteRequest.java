package com.enterprise.makerchecker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * Payload sent by maker-checker-service back to the gateway
 * to execute an approved request.
 */
@Data
public class ExecuteRequest {

    private UUID approvalId;

    @NotBlank
    private String httpMethod;

    @NotBlank
    private String requestPath;

    private String requestBody;
    private Map<String, String> requestHeaders;
    private String queryParams;

    // Original maker context to replay with
    private String makerUserId;
    private String makerUserName;
    private String makerRoles;
    private String makerProductCode;

    // Target service info for direct routing
    private String serviceName;
}
