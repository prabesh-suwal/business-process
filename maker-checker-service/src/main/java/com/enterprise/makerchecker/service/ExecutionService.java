package com.enterprise.makerchecker.service;

import com.enterprise.makerchecker.entity.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes approved maker-checker requests by:
 * 1. Obtaining a JWT for the original maker from cas-server
 * 2. Resolving the target service via Consul
 * 3. Calling the target service with the maker's JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final WebClient.Builder webClientBuilder;
    private final DiscoveryClient discoveryClient;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    public record ExecutionResult(int statusCode, String body) {
    }

    /**
     * Execute an approved request:
     * 1. Get a JWT for the maker from cas-server
     * 2. Resolve target service via Consul
     * 3. Call target with JWT as Authorization header
     */
    public ExecutionResult executeApprovedRequest(ApprovalRequest approval) {
        String serviceName = approval.getConfig().getServiceName();
        String path = approval.getRequestPath();
        String method = approval.getHttpMethod();
        String body = approval.getRequestBody();
        String queryParams = approval.getQueryParams();

        log.info("APPROVAL_EXECUTION_STARTED approvalId={} service={} path={} maker={}",
                approval.getId(), serviceName, path, approval.getMakerUserId());

        // ── 1. Obtain JWT for the maker from cas-server ──
        String makerJwt = obtainMakerJwt(approval.getMakerUserId(), approval.getMakerProductCode());
        if (makerJwt == null) {
            log.error("APPROVAL_EXECUTION_FAILED approvalId={} reason=jwt_generation_failed",
                    approval.getId());
            return new ExecutionResult(500, "Failed to obtain JWT for maker user");
        }

        // ── 2. Resolve target service URL via Consul ──
        String serviceBaseUrl = resolveServiceUrl(serviceName);
        if (serviceBaseUrl == null) {
            log.error("APPROVAL_EXECUTION_FAILED approvalId={} reason=service_not_found service={}",
                    approval.getId(), serviceName);
            return new ExecutionResult(503, "Service not available: " + serviceName);
        }

        String targetUri = serviceBaseUrl + path;
        if (queryParams != null && !queryParams.isEmpty()) {
            targetUri += "?" + queryParams;
        }

        log.info("APPROVAL_EXECUTION_ROUTING approvalId={} target={}", approval.getId(), targetUri);

        try {
            // ── 3. Build and execute the request with maker's JWT ──
            WebClient.RequestBodySpec requestSpec = webClientBuilder.build()
                    .method(HttpMethod.valueOf(method))
                    .uri(targetUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + makerJwt)
                    .header("X-Maker-Checker-Execution", "true")
                    .header("X-Approval-Id", approval.getId().toString());

            var response = executeRequest(requestSpec, body);

            if (response == null) {
                log.error("APPROVAL_EXECUTION_FAILED approvalId={} reason=no_response", approval.getId());
                return new ExecutionResult(500, "No response from target service");
            }

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("APPROVAL_EXECUTION_SUCCESS approvalId={} status={}",
                        approval.getId(), response.statusCode());
            } else {
                log.error("APPROVAL_EXECUTION_FAILED approvalId={} status={} body={}",
                        approval.getId(), response.statusCode(), response.body());
            }

            return response;

        } catch (WebClientResponseException e) {
            log.error("APPROVAL_EXECUTION_FAILED approvalId={} status={} body={}",
                    approval.getId(), e.getStatusCode().value(), e.getResponseBodyAsString());
            return new ExecutionResult(e.getStatusCode().value(), e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("APPROVAL_EXECUTION_FAILED approvalId={} error={}",
                    approval.getId(), e.getMessage(), e);
            return new ExecutionResult(500, "Execution failed: " + e.getMessage());
        }
    }

    /**
     * Call cas-server's internal endpoint to generate a JWT for the maker user.
     */
    private String obtainMakerJwt(String makerUserId, String productCode) {
        String casServerUrl = resolveServiceUrl("cas-server");
        if (casServerUrl == null) {
            log.error("Cannot resolve cas-server from Consul for JWT generation");
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(casServerUrl + "/internal/token/generate")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Service-Token", internalServiceToken)
                    .bodyValue(Map.of(
                            "userId", makerUserId,
                            "productCode", productCode != null ? productCode : "CAS"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response != null && response.containsKey("accessToken")) {
                String token = (String) response.get("accessToken");
                log.info("Obtained JWT for maker user {} product {}", makerUserId, productCode);
                return token;
            }
            log.error("JWT generation response missing accessToken: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Failed to obtain JWT from cas-server for user {}: {}",
                    makerUserId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Execute the HTTP request with or without a body.
     */
    private ExecutionResult executeRequest(WebClient.RequestBodySpec requestSpec, String body) {
        if (body != null && !body.isEmpty()) {
            requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return requestSpec.bodyValue(body)
                    .exchangeToMono(clientResponse -> {
                        int statusCode = clientResponse.statusCode().value();
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ExecutionResult(statusCode, responseBody));
                    })
                    .block(Duration.ofSeconds(30));
        } else {
            return requestSpec
                    .exchangeToMono(clientResponse -> {
                        int statusCode = clientResponse.statusCode().value();
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ExecutionResult(statusCode, responseBody));
                    })
                    .block(Duration.ofSeconds(30));
        }
    }

    /**
     * Resolve downstream service URL from Consul DiscoveryClient.
     */
    private String resolveServiceUrl(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances.isEmpty()) {
                log.error("No Consul instances found for service: {}", serviceName);
                return null;
            }

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
}
