package com.cas.gateway.controller;

import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Internal endpoint that executes approved maker-checker requests.
 * <p>
 * Security:
 * 1. Validates caller via shared secret token (X-Internal-Service-Token)
 * 2. Verifies approval status with maker-checker-service before execution
 * 3. Routes directly to downstream services resolved via Consul DiscoveryClient
 * 4. Logs structured audit events for compliance
 */
@Slf4j
@RestController
public class InternalExecutionController {

        private final DiscoveryClient discoveryClient;
        private final WebClient.Builder webClientBuilder;

        @Value("${internal.service.token}")
        private String internalServiceToken;

        public InternalExecutionController(DiscoveryClient discoveryClient, WebClient.Builder webClientBuilder) {
                this.discoveryClient = discoveryClient;
                this.webClientBuilder = webClientBuilder;
        }

        @PostMapping("/internal/execute-approved-request")
        public Mono<ResponseEntity<String>> executeApprovedRequest(
                        @RequestBody JsonNode request,
                        ServerHttpRequest httpRequest) {

                // ── 1. Token-based authentication ──
                String callerToken = httpRequest.getHeaders().getFirst("X-Internal-Service-Token");
                if (!internalServiceToken.equals(callerToken)) {
                        log.warn("APPROVAL_EXECUTION_REJECTED reason=invalid_token caller={}",
                                        httpRequest.getHeaders().getFirst("X-Service-Name"));
                        return Mono.just(ResponseEntity
                                        .status(HttpStatus.FORBIDDEN)
                                        .body("{\"error\":\"Invalid internal service token\"}"));
                }

                String approvalId = textOrDefault(request.path("approvalId"), "");
                String method = textOrDefault(request.path("httpMethod"), "POST");
                String path = request.path("requestPath").stringValue();
                String body = request.has("requestBody") && !request.get("requestBody").isNull()
                                ? request.get("requestBody").stringValue()
                                : null;
                String queryParams = request.has("queryParams") && !request.get("queryParams").isNull()
                                ? request.get("queryParams").stringValue()
                                : null;
                String serviceName = request.has("serviceName") && !request.get("serviceName").isNull()
                                ? request.get("serviceName").stringValue()
                                : null;
                String makerUserId = textOrDefault(request.path("makerUserId"), "");

                if (serviceName == null || serviceName.isBlank()) {
                        return Mono.just(ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .body("{\"error\":\"serviceName is required\"}"));
                }

                // ── 2. Verify approval with maker-checker-service via Consul ──
                return verifyApproval(approvalId)
                                .flatMap(verified -> {
                                        if (!verified) {
                                                log.warn("APPROVAL_EXECUTION_REJECTED approvalId={} reason=verification_failed",
                                                                approvalId);
                                                return Mono.just(ResponseEntity
                                                                .status(HttpStatus.FORBIDDEN)
                                                                .body("{\"error\":\"Approval verification failed\"}"));
                                        }

                                        // ── 3. Resolve target service URL via Consul (on bounded elastic thread) ──
                                        return resolveServiceUrlAsync(serviceName)
                                                        .flatMap(serviceBaseUrl -> {
                                                                String targetUri = serviceBaseUrl + path;
                                                                if (queryParams != null && !queryParams.isEmpty()) {
                                                                        targetUri += "?" + queryParams;
                                                                }

                                                                log.info("APPROVAL_EXECUTION_STARTED approvalId={} service={} method={} path={} target={} maker={}",
                                                                                approvalId, serviceName, method, path,
                                                                                targetUri, makerUserId);

                                                                // ── 4. Execute the request on downstream service ──
                                                                return executeDownstream(request, approvalId, method,
                                                                                targetUri, body);
                                                        });
                                });
        }

        /**
         * Execute the request on the downstream service.
         */
        private Mono<ResponseEntity<String>> executeDownstream(JsonNode request, String approvalId,
                        String method, String targetUri, String body) {
                WebClient.RequestBodySpec requestSpec = webClientBuilder.build()
                                .method(HttpMethod.valueOf(method))
                                .uri(targetUri);

                // Propagate original maker context headers
                if (request.has("requestHeaders") && request.get("requestHeaders").isObject()) {
                        request.get("requestHeaders").properties().forEach(entry -> {
                                String headerName = entry.getKey();
                                if (!headerName.equalsIgnoreCase("Host") &&
                                                !headerName.equalsIgnoreCase("Content-Length") &&
                                                !headerName.equalsIgnoreCase("Transfer-Encoding")) {
                                        requestSpec.header(headerName,
                                                        entry.getValue().stringValue());
                                }
                        });
                }

                // Set maker's user context headers
                setIfPresent(requestSpec, "X-User-Id",
                                request.path("makerUserId").stringValue());
                setIfPresent(requestSpec, "X-User-Name",
                                request.path("makerUserName").stringValue());
                setIfPresent(requestSpec, "X-Roles", request.path("makerRoles").stringValue());
                setIfPresent(requestSpec, "X-Product-Code",
                                request.path("makerProductCode").stringValue());

                // Mark as internal execution (prevents maker-checker filter loop)
                requestSpec.header("X-Maker-Checker-Execution", "true");
                requestSpec.header("X-Approval-Id", approvalId);

                // Execute
                Mono<ResponseEntity<String>> responseMono;
                if (body != null && !body.isEmpty()) {
                        requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                        responseMono = requestSpec.bodyValue(body).retrieve().toEntity(String.class);
                } else {
                        responseMono = requestSpec.retrieve().toEntity(String.class);
                }

                return responseMono
                                .map(response -> {
                                        log.info("APPROVAL_EXECUTION_SUCCESS approvalId={} status={}",
                                                        approvalId, response.getStatusCode().value());
                                        return ResponseEntity
                                                        .status(response.getStatusCode())
                                                        .body(response.getBody());
                                })
                                .onErrorResume(e -> handleExecutionError(approvalId, e));
        }

        /**
         * Resolve downstream service URL from Consul DiscoveryClient asynchronously.
         * Offloads the blocking getInstances() call to a bounded elastic thread.
         */
        private Mono<String> resolveServiceUrlAsync(String serviceName) {
                return Mono.fromCallable(() -> resolveServiceUrl(serviceName))
                                .subscribeOn(Schedulers.boundedElastic())
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("APPROVAL_EXECUTION_FAILED reason=service_not_found service={}",
                                                        serviceName);
                                        return Mono.error(new RuntimeException(
                                                        "Service not available: " + serviceName));
                                }))
                                .onErrorResume(e -> {
                                        log.error("APPROVAL_EXECUTION_FAILED reason=service_not_found service={}",
                                                        serviceName);
                                        return Mono.just(ResponseEntity
                                                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                                                        .body("{\"error\":\"Service not available: " + serviceName
                                                                        + "\"}"))
                                                        .flatMap(resp -> Mono.error(new RuntimeException(
                                                                        "Service not available: " + serviceName)));
                                });
        }

        /**
         * Resolve downstream service URL from Consul DiscoveryClient.
         * Picks a random instance for basic load balancing.
         * Returns null if no instances found.
         */
        private String resolveServiceUrl(String serviceName) {
                try {
                        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                        if (instances.isEmpty()) {
                                log.error("No Consul instances found for service: {}", serviceName);
                                return null;
                        }

                        // Random instance selection for basic load balancing
                        ServiceInstance instance = instances.size() == 1
                                        ? instances.get(0)
                                        : instances.get(ThreadLocalRandom.current().nextInt(instances.size()));

                        String url = instance.getUri().toString();
                        log.debug("Resolved service {} -> {} (from {} instances)", serviceName, url, instances.size());
                        return url;
                } catch (Exception e) {
                        log.error("Failed to resolve service {} from Consul: {}", serviceName, e.getMessage());
                        return null;
                }
        }

        /**
         * Verify approval with maker-checker-service via Consul-resolved URL.
         * Offloads the blocking Consul lookup to a bounded elastic thread.
         */
        private Mono<Boolean> verifyApproval(String approvalId) {
                if (approvalId == null || approvalId.isBlank()) {
                        return Mono.just(false);
                }

                return Mono.fromCallable(() -> resolveServiceUrl("maker-checker-service"))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(mcServiceUrl -> {
                                        if (mcServiceUrl == null) {
                                                log.error("Cannot verify approval — maker-checker-service not found in Consul");
                                                return Mono.just(false);
                                        }

                                        return webClientBuilder.build()
                                                        .get()
                                                        .uri(mcServiceUrl + "/internal/approvals/" + approvalId
                                                                        + "/verify")
                                                        .header("X-Internal-Service-Token", internalServiceToken)
                                                        .retrieve()
                                                        .bodyToMono(JsonNode.class)
                                                        .map(json -> json.path("valid").booleanValue())
                                                        .onErrorResume(e -> {
                                                                log.error("Approval verification call failed for {}: {}",
                                                                                approvalId, e.getMessage());
                                                                return Mono.just(false);
                                                        });
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.error("Cannot verify approval — maker-checker-service not found in Consul");
                                        return Mono.just(false);
                                }));
        }

        private Mono<ResponseEntity<String>> handleExecutionError(
                        String approvalId, Throwable e) {
                log.error("APPROVAL_EXECUTION_FAILED approvalId={} error={}", approvalId, e.getMessage());
                return Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("{\"error\":\"Execution failed: " + e.getMessage() + "\"}"));
        }

        private void setIfPresent(WebClient.RequestBodySpec spec, String header, String value) {
                if (value != null && !value.isEmpty()) {
                        spec.header(header, value);
                }
        }

        /**
         * Null-safe text extraction with default value (replaces deprecated
         * asText(String))
         */
        private String textOrDefault(JsonNode node, String defaultValue) {
                String val = node.stringValue();
                return val != null ? val : defaultValue;
        }
}
