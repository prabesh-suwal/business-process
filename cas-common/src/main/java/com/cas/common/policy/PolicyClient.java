package com.cas.common.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for calling policy-engine-service.
 * Microservices use this to evaluate authorization decisions.
 */
@Slf4j
public class PolicyClient {

    private final String policyEngineUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PolicyClient(String policyEngineUrl) {
        this.policyEngineUrl = policyEngineUrl.endsWith("/")
                ? policyEngineUrl.substring(0, policyEngineUrl.length() - 1)
                : policyEngineUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Evaluate a policy request.
     * 
     * @param request The evaluation request
     * @return Evaluation response with allow/deny decision
     */
    public PolicyEvaluationResponse evaluate(PolicyEvaluationRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(policyEngineUrl + "/api/evaluate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), PolicyEvaluationResponse.class);
            } else {
                log.error("Policy evaluation failed with status {}: {}",
                        response.statusCode(), response.body());
                // Default deny on error
                return PolicyEvaluationResponse.builder()
                        .allowed(false)
                        .effect("DENY")
                        .reason("Policy evaluation service error: " + response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to evaluate policy", e);
            // Default deny on error
            return PolicyEvaluationResponse.builder()
                    .allowed(false)
                    .effect("DENY")
                    .reason("Policy evaluation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Dry-run evaluation (for testing).
     */
    public PolicyEvaluationResponse evaluateDryRun(PolicyEvaluationRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(policyEngineUrl + "/api/evaluate/dry-run"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), PolicyEvaluationResponse.class);
            } else {
                return PolicyEvaluationResponse.builder()
                        .allowed(false)
                        .effect("DENY")
                        .reason("Dry-run failed: " + response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to evaluate policy (dry-run)", e);
            return PolicyEvaluationResponse.builder()
                    .allowed(false)
                    .effect("DENY")
                    .reason("Dry-run failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Health check for policy engine.
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(policyEngineUrl + "/api/evaluate/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Policy engine health check failed", e);
            return false;
        }
    }
}
