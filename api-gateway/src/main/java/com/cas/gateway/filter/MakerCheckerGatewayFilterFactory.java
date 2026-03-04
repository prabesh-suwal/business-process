package com.cas.gateway.filter;

import com.cas.common.dto.ApiResponse;
import com.cas.gateway.service.MakerCheckerService;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway filter that intercepts requests and checks if they require
 * maker-checker approval. If so, the request body is captured and stored
 * as a pending approval in the maker-checker-service. The original request
 * is NOT forwarded to the downstream service.
 *
 * Only applies to write operations (POST, PUT, PATCH, DELETE).
 */
@Slf4j
@Component
public class MakerCheckerGatewayFilterFactory
        extends AbstractGatewayFilterFactory<MakerCheckerGatewayFilterFactory.Config> {

    private final MakerCheckerService makerCheckerService;
    private final ObjectMapper objectMapper;

    public MakerCheckerGatewayFilterFactory(MakerCheckerService makerCheckerService, ObjectMapper objectMapper) {
        super(Config.class);
        this.makerCheckerService = makerCheckerService;
        this.objectMapper = objectMapper;
    }

    public static class Config {
        // Currently empty — all config is in maker-checker-service DB
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpMethod method = request.getMethod();

            // Skip replayed approved requests (from InternalExecutionController)
            if ("true".equals(request.getHeaders().getFirst("X-Maker-Checker-Execution"))) {
                log.debug("MakerChecker filter: skipping replayed approved request {} {}", method,
                        request.getURI().getPath());
                return chain.filter(exchange);
            }

            // Only intercept write operations
            if (method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            String path = request.getURI().getPath();
            String httpMethod = method.name();

            // Check with maker-checker-service if this endpoint needs approval
            log.debug("MakerChecker filter: checking {} {}", httpMethod, path);
            return makerCheckerService.checkConfig(httpMethod, path)
                    .flatMap(checkResult -> {
                        log.debug("MakerChecker check result for {} {}: {}", httpMethod, path, checkResult);
                        Object requiredObj = checkResult.get("required");
                        boolean required = Boolean.TRUE.equals(requiredObj);

                        if (!required) {
                            log.debug("MakerChecker not required for {} {}", httpMethod, path);
                            return chain.filter(exchange);
                        }

                        log.info("Maker-checker required for {} {} — intercepting request", httpMethod, path);

                        // Capture the request body and create an approval
                        return captureBodyAndCreateApproval(exchange, httpMethod, path);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.debug("MakerChecker check returned empty for {} {} — passing through", httpMethod, path);
                        return chain.filter(exchange);
                    })); // If check fails, let the request through
        };
    }

    private Mono<Void> captureBodyAndCreateApproval(ServerWebExchange exchange,
            String httpMethod, String path) {
        ServerHttpRequest request = exchange.getRequest();

        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .defaultIfEmpty("")
                .flatMap(body -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("httpMethod", httpMethod);
                    payload.put("requestPath", path);
                    payload.put("requestBody", body);
                    payload.put("queryParams", request.getURI().getRawQuery());

                    // Capture relevant headers for replay
                    Map<String, String> headers = new HashMap<>();
                    request.getHeaders().forEach((key, values) -> {
                        if (key.startsWith("X-") || key.equalsIgnoreCase("Content-Type") ||
                                key.equalsIgnoreCase("Accept")) {
                            headers.put(key, String.join(",", values));
                        }
                    });
                    payload.put("requestHeaders", headers);

                    // Maker context from gateway-injected headers
                    payload.put("makerUserId", getHeader(request, "X-User-Id"));
                    payload.put("makerUserName", getHeader(request, "X-User-Name"));
                    payload.put("makerRoles", getHeader(request, "X-Roles"));
                    payload.put("makerProductCode", getHeader(request, "X-Product-Code"));

                    return makerCheckerService.createApproval(payload)
                            .flatMap(result -> respondWithAccepted(exchange, result));
                });
    }

    private Mono<Void> respondWithAccepted(ServerWebExchange exchange, Map<String, Object> result) {
        exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Close connection to prevent keep-alive from reusing a half-consumed request
        // stream
        exchange.getResponse().getHeaders().set("Connection", "close");

        Object id = result.get("id");
        String approvalId = id != null ? id.toString() : "unknown";
        Map<String, String> data = Map.of("approvalId", approvalId);
        ApiResponse<Map<String, String>> apiResponse = ApiResponse.success(data, "Request submitted for approval");

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            exchange.getResponse().getHeaders().setContentLength(bytes.length);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to serialize approval response", e);
            return exchange.getResponse().setComplete();
        }
    }

    private String getHeader(ServerHttpRequest request, String headerName) {
        String value = request.getHeaders().getFirst(headerName);
        return value != null ? value : "";
    }
}
