package com.cas.common.audit.gateway;

import com.cas.common.audit.ActorType;
import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import com.cas.common.audit.AuditEvent;
import com.cas.common.audit.CorrelationIdFilter;
import com.cas.common.security.CasHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Enhanced global filter for audit logging of all gateway requests.
 * Logs API access events to the centralized audit service asynchronously.
 * <p>
 * Features:
 * - Correlation ID generation and propagation
 * - Actor extraction from JWT headers (after JWT filter processes)
 * - Error detail logging for non-2xx responses
 * - Optional request/response body logging with sensitive field masking
 * <p>
 * To use this filter, create a @Bean in your gateway configuration:
 * 
 * <pre>
 * &#64;Bean
 * public GatewayAuditFilter gatewayAuditFilter() {
 *     return new GatewayAuditFilter("PRODUCT_CODE", "http://localhost:9002", properties);
 * }
 * </pre>
 */
@Slf4j
public class GatewayAuditFilter implements GlobalFilter, Ordered {

    private final String productCode;
    private final String auditServiceUrl;
    private final GatewayAuditProperties properties;
    private final ObjectMapper objectMapper;
    private volatile WebClient webClient;

    public GatewayAuditFilter(String productCode, String auditServiceUrl, GatewayAuditProperties properties) {
        this.productCode = productCode;
        this.auditServiceUrl = auditServiceUrl;
        this.properties = properties != null ? properties : new GatewayAuditProperties();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor for backward compatibility without properties.
     */
    public GatewayAuditFilter(String productCode, String auditServiceUrl) {
        this(productCode, auditServiceUrl, new GatewayAuditProperties());
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    webClient = WebClient.builder()
                            .baseUrl(auditServiceUrl)
                            .build();
                }
            }
        }
        return webClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();

        // Skip audit for health checks and actuator endpoints
        String path = request.getPath().value();
        if (path.startsWith("/actuator") || path.endsWith("/health")) {
            return chain.filter(exchange);
        }

        // Generate or propagate correlation ID
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        // Add correlation ID to the request for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-Id", finalCorrelationId)
                .build();

        // Store start time and correlation ID in exchange attributes
        exchange.getAttributes().put("correlationId", finalCorrelationId);
        exchange.getAttributes().put("auditStartTime", startTime);

        // Check if we should log request body
        boolean shouldLogRequestBody = shouldLogBody(path, true);
        boolean shouldLogResponseBody = shouldLogBody(path, false);

        ServerWebExchange mutatedExchange;

        if (shouldLogRequestBody || shouldLogResponseBody) {
            // Wrap request to capture body if needed
            ServerHttpRequest wrappedRequest = shouldLogRequestBody
                    ? new CachingServerHttpRequestDecorator(mutatedRequest, exchange)
                    : mutatedRequest;

            // Wrap response to capture body if needed
            ServerHttpResponse wrappedResponse = shouldLogResponseBody || properties.isLogErrorDetails()
                    ? new CachingServerHttpResponseDecorator(exchange.getResponse(), exchange,
                            properties.getMaxBodySize())
                    : exchange.getResponse();

            mutatedExchange = exchange.mutate()
                    .request(wrappedRequest)
                    .response(wrappedResponse)
                    .build();
        } else {
            mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
        }

        // Add correlation ID to response header for client tracing
        mutatedExchange.getResponse().getHeaders().add("X-Correlation-Id", finalCorrelationId);

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx.put(CorrelationIdFilter.CORRELATION_ID_KEY, finalCorrelationId))
                .doFirst(() -> MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, finalCorrelationId))
                .doFinally(signalType -> {
                    MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
                    // Log audit event asynchronously after response
                    try {
                        publishAuditEvent(mutatedExchange, startTime);
                    } catch (Exception e) {
                        log.warn("Failed to publish gateway audit event: {}", e.getMessage());
                    }
                });
    }

    private boolean shouldLogBody(String path, boolean isRequest) {
        // Check if feature is enabled
        if (isRequest && !properties.isLogRequestBody()) {
            return false;
        }
        if (!isRequest && !properties.isLogResponseBody() && !properties.isLogErrorDetails()) {
            return false;
        }

        // Check exclusion patterns
        for (String pattern : properties.getExcludeBodyPatterns()) {
            if (path.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    private void publishAuditEvent(ServerWebExchange exchange, Instant startTime) {
        ServerHttpRequest request = exchange.getRequest();
        HttpStatus status = exchange.getResponse().getStatusCode() != null
                ? HttpStatus.valueOf(exchange.getResponse().getStatusCode().value())
                : HttpStatus.OK;

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();

        // Extract user info from headers (set by JWT filter which runs before us at
        // order -100)
        String userId = request.getHeaders().getFirst(CasHeaders.USER_ID);
        String userEmail = request.getHeaders().getFirst(CasHeaders.USER_EMAIL);
        String userName = request.getHeaders().getFirst(CasHeaders.USER_NAME);
        String tokenType = request.getHeaders().getFirst(CasHeaders.TOKEN_TYPE);
        String roles = request.getHeaders().getFirst(CasHeaders.ROLES);
        String correlationId = (String) exchange.getAttributes().get("correlationId");

        // Determine action based on status code
        AuditAction action = status.is2xxSuccessful() || status.is3xxRedirection()
                ? AuditAction.ACCESS_GRANTED
                : AuditAction.ACCESS_DENIED;

        // Build metadata map
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", request.getMethod() != null ? request.getMethod().name() : "UNKNOWN");
        metadata.put("path", request.getPath().value());
        metadata.put("statusCode", status.value());
        metadata.put("durationMs", durationMs);
        metadata.put("userAgent", request.getHeaders().getFirst("User-Agent") != null
                ? request.getHeaders().getFirst("User-Agent")
                : "unknown");

        // Add request body if captured
        String requestBody = (String) exchange.getAttributes().get("capturedRequestBody");
        if (requestBody != null && !requestBody.isEmpty()) {
            metadata.put("requestBody", maskSensitiveFields(requestBody));
        }

        // Add response body if captured (for errors or when enabled)
        String responseBody = (String) exchange.getAttributes().get("capturedResponseBody");
        if (responseBody != null && !responseBody.isEmpty()) {
            boolean isError = !status.is2xxSuccessful() && !status.is3xxRedirection();
            if (properties.isLogResponseBody() || (properties.isLogErrorDetails() && isError)) {
                metadata.put("responseBody", maskSensitiveFields(responseBody));
            }
        }

        // Build error message for non-2xx responses
        String errorMessage = null;
        if (!status.is2xxSuccessful() && !status.is3xxRedirection()) {
            errorMessage = String.format("HTTP %d: %s", status.value(), status.getReasonPhrase());
            if (responseBody != null && !responseBody.isEmpty() && responseBody.length() < 500) {
                // Include truncated error response for context
                errorMessage += " - " + responseBody.substring(0, Math.min(responseBody.length(), 200));
            }
        }

        // Build the audit event
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .serviceName(productCode + "-gateway")
                .productCode(productCode)
                .correlationId(correlationId)
                .actorId(userId)
                .actorType(mapTokenTypeToActorType(tokenType))
                .actorName(userName)
                .actorEmail(userEmail)
                .actorRoles(roles != null ? Arrays.asList(roles.split(",")) : null)
                .ipAddress(getClientIp(request))
                .action(action)
                .category(AuditCategory.ACCESS)
                .resourceType("API")
                .resourceId(request.getPath().value())
                .description(String.format("%s %s -> %d (%dms)",
                        request.getMethod(),
                        request.getPath().value(),
                        status.value(),
                        durationMs))
                .failureReason(errorMessage)
                .metadata(metadata)
                .build();

        // Publish asynchronously (fire and forget)
        getWebClient()
                .post()
                .uri("/api/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(3000))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> log.debug("Published gateway audit event: {} {}",
                                request.getMethod(), request.getPath().value()),
                        error -> log.warn("Failed to publish gateway audit event: {}", error.getMessage()));
    }

    /**
     * Mask sensitive fields in JSON bodies.
     */
    private String maskSensitiveFields(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        try {
            // Try to parse as JSON
            JsonNode rootNode = objectMapper.readTree(body);
            if (rootNode.isObject()) {
                maskNode((ObjectNode) rootNode);
                return objectMapper.writeValueAsString(rootNode);
            }
        } catch (Exception e) {
            // Not JSON, try simple string replacement
            for (String field : properties.getSensitiveFields()) {
                // Mask patterns like "field":"value" or "field": "value"
                body = body.replaceAll(
                        "\"" + field + "\"\\s*:\\s*\"[^\"]*\"",
                        "\"" + field + "\":\"***MASKED***\"");
            }
        }
        return body;
    }

    private void maskNode(ObjectNode node) {
        Iterator<String> fieldNames = node.fieldNames();
        List<String> fieldsToMask = new ArrayList<>();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode child = node.get(fieldName);

            // Check if this field should be masked
            for (String sensitive : properties.getSensitiveFields()) {
                if (fieldName.toLowerCase().contains(sensitive.toLowerCase())) {
                    fieldsToMask.add(fieldName);
                    break;
                }
            }

            // Recursively process child objects
            if (child.isObject()) {
                maskNode((ObjectNode) child);
            }
        }

        // Apply masking
        for (String field : fieldsToMask) {
            node.put(field, "***MASKED***");
        }
    }

    private ActorType mapTokenTypeToActorType(String tokenType) {
        if (tokenType == null) {
            return ActorType.ANONYMOUS;
        }
        return switch (tokenType.toUpperCase()) {
            case "USER" -> ActorType.USER;
            case "SERVICE" -> ActorType.API_CLIENT;
            case "SYSTEM" -> ActorType.SYSTEM;
            default -> ActorType.ANONYMOUS;
        };
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            if (request.getRemoteAddress() != null) {
                ip = request.getRemoteAddress().getAddress().getHostAddress();
            }
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    @Override
    public int getOrder() {
        // Run AFTER JWT authentication filter (which is at -100)
        // This ensures user headers are populated when we capture them
        return -50;
    }

    /**
     * Decorator to cache request body for audit logging.
     */
    private class CachingServerHttpRequestDecorator extends ServerHttpRequestDecorator {
        private final ServerWebExchange exchange;

        public CachingServerHttpRequestDecorator(ServerHttpRequest delegate, ServerWebExchange exchange) {
            super(delegate);
            this.exchange = exchange;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return DataBufferUtils.join(super.getBody())
                    .flatMapMany(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        String body = new String(bytes, StandardCharsets.UTF_8);
                        if (body.length() <= properties.getMaxBodySize()) {
                            exchange.getAttributes().put("capturedRequestBody", body);
                        } else {
                            exchange.getAttributes().put("capturedRequestBody",
                                    body.substring(0, properties.getMaxBodySize()) + "...[TRUNCATED]");
                        }

                        DataBufferFactory factory = new DefaultDataBufferFactory();
                        return Flux.just(factory.wrap(bytes));
                    });
        }
    }

    /**
     * Decorator to cache response body for audit logging.
     */
    private class CachingServerHttpResponseDecorator extends ServerHttpResponseDecorator {
        private final ServerWebExchange exchange;
        private final int maxSize;

        public CachingServerHttpResponseDecorator(ServerHttpResponse delegate,
                ServerWebExchange exchange, int maxSize) {
            super(delegate);
            this.exchange = exchange;
            this.maxSize = maxSize;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            return super.writeWith(Flux.from(body)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        String responseBody = new String(bytes, StandardCharsets.UTF_8);
                        if (responseBody.length() <= maxSize) {
                            exchange.getAttributes().put("capturedResponseBody", responseBody);
                        } else {
                            exchange.getAttributes().put("capturedResponseBody",
                                    responseBody.substring(0, maxSize) + "...[TRUNCATED]");
                        }

                        DataBufferFactory factory = getDelegate().bufferFactory();
                        return factory.wrap(bytes);
                    }));
        }
    }
}
