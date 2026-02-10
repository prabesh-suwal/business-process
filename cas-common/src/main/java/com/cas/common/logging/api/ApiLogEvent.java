package com.cas.common.logging.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * API Log event for technical/system logging.
 * Captured automatically by gateway filters for debugging, performance
 * monitoring.
 * 
 * Retention: 30-90 days
 * Audience: Dev, DevOps, SRE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLogEvent {

    @Builder.Default
    private String logId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String correlationId;
    private String traceId;
    private String serviceName;
    private String instanceId;
    private String environment;

    // Request Details
    private String httpMethod;
    private String endpoint;
    private String fullPath;
    private Map<String, String> queryParams;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private String clientIp;
    private String userAgent;
    private String authenticatedUserId;
    private String userRole;

    // Response Details
    private Integer responseStatus;
    private Long responseTimeMs;
    private String responseBody;
    private String errorCode;
    private String errorMessage;
    private String exceptionClass;
    private String stackTrace;

    // Routing Info
    private String upstreamService;
    private String downstreamService;
    private String externalApiCalled;
    private Integer retryCount;
}
