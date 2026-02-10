package com.cas.common.logging.gateway;

import com.cas.common.audit.CorrelationIdFilter;
import com.cas.common.logging.api.ApiLogEvent;
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
 * Gateway filter for API logging (technical/system logs).
 * Publishes API requests/responses to the audit-service's /api/logs/api
 * endpoint.
 * 
 * This is separate from audit logging - API logs are for debugging, performance
 * monitoring, and operational visibility. Retention: 30-90 days.
 * 
 * Usage:
 * 
 * <pre>
 * &#64;Bean
 * public GatewayApiLogFilter gatewayApiLogFilter() {
 *     return new GatewayApiLogFilter("PRODUCT_CODE", "http://localhost:9002", properties);
 * }
 * </pre>
 */
@Slf4j
public class GatewayApiLogFilter implements GlobalFilter, Ordered {

    private final String serviceName;
    private final String auditServiceUrl;
    private final GatewayApiLogProperties properties;
    private final ObjectMapper objectMapper;
    private volatile WebClient webClient;

    public GatewayApiLogFilter(String serviceName, String auditServiceUrl, GatewayApiLogProperties properties) {
        this.serviceName = serviceName;
        this.auditServiceUrl = auditServiceUrl;
        this.properties = properties != null ? properties : new GatewayApiLogProperties();
        this.objectMapper = new ObjectMapper();
    }

    public GatewayApiLogFilter(String serviceName, String auditServiceUrl) {
        this(serviceName, auditServiceUrl, new GatewayApiLogProperties());
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

        // Skip logging for health checks and actuator endpoints
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

        // Add correlation ID to the request
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-Id", finalCorrelationId)
                .build();

        // Store attributes
        exchange.getAttributes().put("correlationId", finalCorrelationId);
        exchange.getAttributes().put("apiLogStartTime", startTime);

        boolean shouldLogRequestBody = shouldLogBody(path, true);
        boolean shouldLogResponseBody = shouldLogBody(path, false);

        ServerWebExchange mutatedExchange;

        if (shouldLogRequestBody || shouldLogResponseBody) {
            ServerHttpRequest wrappedRequest = shouldLogRequestBody
                    ? new CachingServerHttpRequestDecorator(mutatedRequest, exchange)
                    : mutatedRequest;

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

        mutatedExchange.getResponse().getHeaders().add("X-Correlation-Id", finalCorrelationId);

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx.put(CorrelationIdFilter.CORRELATION_ID_KEY, finalCorrelationId))
                .doFirst(() -> MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, finalCorrelationId))
                .doFinally(signalType -> {
                    MDC.remove(CorrelationIdFilter.CORRELATION_ID_KEY);
                    try {
                        publishApiLogEvent(mutatedExchange, startTime);
                    } catch (Exception e) {
                        log.warn("Failed to publish API log event: {}", e.getMessage());
                    }
                });
    }

    private boolean shouldLogBody(String path, boolean isRequest) {
        if (isRequest && !properties.isLogRequestBody()) {
            return false;
        }
        if (!isRequest && !properties.isLogResponseBody() && !properties.isLogErrorDetails()) {
            return false;
        }

        for (String pattern : properties.getExcludeBodyPatterns()) {
            if (path.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    private void publishApiLogEvent(ServerWebExchange exchange, Instant startTime) {
        ServerHttpRequest request = exchange.getRequest();
        HttpStatus status = exchange.getResponse().getStatusCode() != null
                ? HttpStatus.valueOf(exchange.getResponse().getStatusCode().value())
                : HttpStatus.OK;

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();

        String userId = request.getHeaders().getFirst(CasHeaders.USER_ID);
        String roles = request.getHeaders().getFirst(CasHeaders.ROLES);
        String correlationId = (String) exchange.getAttributes().get("correlationId");

        // Build query params map
        Map<String, String> queryParams = new HashMap<>();
        request.getQueryParams().forEach((k, v) -> queryParams.put(k, String.join(",", v)));

        // Build request headers map (filtered)
        Map<String, String> requestHeaders = new HashMap<>();
        request.getHeaders().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Authorization") && !k.equalsIgnoreCase("Cookie")) {
                requestHeaders.put(k, String.join(",", v));
            }
        });

        // Get captured bodies
        String requestBody = (String) exchange.getAttributes().get("capturedRequestBody");
        String responseBody = (String) exchange.getAttributes().get("capturedResponseBody");

        // Mask sensitive fields
        if (requestBody != null) {
            requestBody = maskSensitiveFields(requestBody);
        }
        if (responseBody != null) {
            responseBody = maskSensitiveFields(responseBody);
        }

        // Build error info
        String errorMessage = null;
        if (!status.is2xxSuccessful() && !status.is3xxRedirection()) {
            errorMessage = String.format("HTTP %d: %s", status.value(), status.getReasonPhrase());
        }

        ApiLogEvent event = ApiLogEvent.builder()
                .correlationId(correlationId)
                .serviceName(serviceName + "-gateway")
                .httpMethod(request.getMethod() != null ? request.getMethod().name() : "UNKNOWN")
                .endpoint(extractEndpoint(request.getPath().value()))
                .fullPath(request.getPath().value())
                .queryParams(queryParams.isEmpty() ? null : queryParams)
                .requestHeaders(requestHeaders.isEmpty() ? null : requestHeaders)
                .requestBody(requestBody)
                .clientIp(getClientIp(request))
                .userAgent(request.getHeaders().getFirst("User-Agent"))
                .authenticatedUserId(userId)
                .userRole(roles)
                .responseStatus(status.value())
                .responseTimeMs(durationMs)
                .responseBody(shouldReturnResponseBody(status) ? responseBody : null)
                .errorMessage(errorMessage)
                .build();

        // Publish asynchronously
        getWebClient()
                .post()
                .uri("/api/logs/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(3000))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> log.debug("Published API log: {} {} -> {}",
                                request.getMethod(), request.getPath().value(), status.value()),
                        error -> log.warn("Failed to publish API log: {}", error.getMessage()));
    }

    private String extractEndpoint(String path) {
        // Convert /api/users/123 to /api/users/{id}
        return path.replaceAll("/[0-9a-fA-F-]{36}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }

    private boolean shouldReturnResponseBody(HttpStatus status) {
        return properties.isLogResponseBody() ||
                (properties.isLogErrorDetails() && !status.is2xxSuccessful() && !status.is3xxRedirection());
    }

    private String maskSensitiveFields(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(body);
            if (rootNode.isObject()) {
                maskNode((ObjectNode) rootNode);
                return objectMapper.writeValueAsString(rootNode);
            }
        } catch (Exception e) {
            for (String field : properties.getSensitiveFields()) {
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

            for (String sensitive : properties.getSensitiveFields()) {
                if (fieldName.toLowerCase().contains(sensitive.toLowerCase())) {
                    fieldsToMask.add(fieldName);
                    break;
                }
            }

            if (child.isObject()) {
                maskNode((ObjectNode) child);
            }
        }

        for (String field : fieldsToMask) {
            node.put(field, "***MASKED***");
        }
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
        return -50;
    }

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

    private class CachingServerHttpResponseDecorator extends ServerHttpResponseDecorator {
        private final ServerWebExchange exchange;
        private final int maxSize;

        public CachingServerHttpResponseDecorator(ServerHttpResponse delegate, ServerWebExchange exchange,
                int maxSize) {
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
