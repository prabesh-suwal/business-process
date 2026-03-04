package com.enterprise.makerchecker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * Payload sent by the gateway to create a new approval request.
 */
@Data
public class CreateApprovalRequest {

    @NotBlank
    private String httpMethod;

    @NotBlank
    private String requestPath;

    private String requestBody;
    private Map<String, String> requestHeaders;
    private String queryParams;

    // Maker context from gateway X-headers
    @NotBlank
    private String makerUserId;

    private String makerUserName;
    private String makerRoles;
    private String makerProductCode;
}
