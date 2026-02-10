package com.cas.common.logging.gateway;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for GatewayApiLogFilter.
 */
@Data
public class GatewayApiLogProperties {

    /**
     * Whether to log request bodies.
     */
    private boolean logRequestBody = false;

    /**
     * Whether to log response bodies.
     */
    private boolean logResponseBody = false;

    /**
     * Whether to log details for error responses.
     */
    private boolean logErrorDetails = true;

    /**
     * Maximum body size to capture (in characters).
     */
    private int maxBodySize = 4096;

    /**
     * URL patterns to exclude from body logging.
     */
    private List<String> excludeBodyPatterns = Arrays.asList(
            "/api/files/",
            "/api/documents/",
            "/api/uploads/");

    /**
     * Sensitive field names to mask in logs.
     */
    private List<String> sensitiveFields = Arrays.asList(
            "password",
            "secret",
            "token",
            "authorization",
            "apiKey",
            "api_key",
            "credential",
            "ssn",
            "creditCard",
            "credit_card");
}
