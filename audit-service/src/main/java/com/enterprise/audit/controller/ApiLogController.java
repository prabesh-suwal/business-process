package com.enterprise.audit.controller;

import com.cas.common.logging.api.ApiLogEvent;
import com.enterprise.audit.entity.ApiLogEntity;
import com.enterprise.audit.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Controller for API logs (technical/system logs).
 * Handles ingestion and querying of API logs from gateways and services.
 */
@RestController
@RequestMapping("/api/logs/api")
@RequiredArgsConstructor
@Slf4j
public class ApiLogController {

    private final ApiLogRepository apiLogRepository;

    /**
     * Ingest a new API log event.
     */
    @PostMapping
    public ResponseEntity<Void> ingest(@RequestBody ApiLogEvent event) {
        log.trace("Ingesting API log: {}", event.getLogId());

        ApiLogEntity entity = ApiLogEntity.builder()
                .logId(UUID.fromString(event.getLogId()))
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .correlationId(event.getCorrelationId())
                .traceId(event.getTraceId())
                .serviceName(event.getServiceName())
                .instanceId(event.getInstanceId())
                .environment(event.getEnvironment())
                .httpMethod(event.getHttpMethod())
                .endpoint(event.getEndpoint())
                .fullPath(event.getFullPath())
                .queryParams(event.getQueryParams() != null ? event.getQueryParams().toString() : null)
                .requestHeaders(event.getRequestHeaders() != null ? event.getRequestHeaders().toString() : null)
                .requestBody(event.getRequestBody())
                .clientIp(event.getClientIp())
                .userAgent(event.getUserAgent())
                .authenticatedUserId(event.getAuthenticatedUserId())
                .userRole(event.getUserRole())
                .responseStatus(event.getResponseStatus())
                .responseTimeMs(event.getResponseTimeMs())
                .responseBody(event.getResponseBody())
                .errorCode(event.getErrorCode())
                .errorMessage(event.getErrorMessage())
                .exceptionClass(event.getExceptionClass())
                .stackTrace(event.getStackTrace())
                .upstreamService(event.getUpstreamService())
                .downstreamService(event.getDownstreamService())
                .externalApiCalled(event.getExternalApiCalled())
                .retryCount(event.getRetryCount())
                .build();

        apiLogRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    /**
     * Search API logs with filters.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ApiLogEntity>> search(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ApiLogEntity> logs = apiLogRepository.searchLogs(
                serviceName, correlationId, httpMethod, responseStatus, startTime, endTime, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get a specific API log by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiLogEntity> getById(@PathVariable UUID id) {
        return apiLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
