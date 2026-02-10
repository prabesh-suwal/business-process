package com.enterprise.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * API Log entity for technical/system logging.
 * Used for debugging, performance monitoring, and incident investigation.
 * Retention: 30-90 days.
 */
@Entity
@Table(name = "api_logs", indexes = {
        @Index(name = "idx_api_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_api_logs_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_api_logs_service_name", columnList = "service_name"),
        @Index(name = "idx_api_logs_response_status", columnList = "response_status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID logId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "trace_id", length = 50)
    private String traceId;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "instance_id", length = 100)
    private String instanceId;

    @Column(length = 20)
    private String environment;

    // Request Details
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "full_path", length = 2000)
    private String fullPath;

    @Column(name = "query_params", columnDefinition = "JSONB")
    private String queryParams;

    @Column(name = "request_headers", columnDefinition = "JSONB")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "authenticated_user_id", length = 100)
    private String authenticatedUserId;

    @Column(name = "user_role", length = 100)
    private String userRole;

    // Response Details
    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "exception_class", length = 200)
    private String exceptionClass;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    // Routing Info
    @Column(name = "upstream_service", length = 100)
    private String upstreamService;

    @Column(name = "downstream_service", length = 100)
    private String downstreamService;

    @Column(name = "external_api_called", length = 500)
    private String externalApiCalled;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
}
