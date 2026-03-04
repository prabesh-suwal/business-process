package com.cas.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive WebClient service for the gateway to communicate with
 * maker-checker-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MakerCheckerService {

        private final WebClient.Builder webClientBuilder;

        @Value("${gateway.routes.maker-checker-url:http://localhost:9011}")
        private String makerCheckerUrl;

        private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
        };

        /**
         * Check if a given endpoint requires maker-checker approval.
         *
         * @return Mono with check result, or empty if no config found / service
         *         unavailable
         */
        public Mono<Map<String, Object>> checkConfig(String httpMethod, String requestPath) {
                return webClientBuilder.build()
                                .get()
                                .uri(makerCheckerUrl
                                                + "/api/maker-checker/configs/check?httpMethod={method}&requestPath={path}",
                                                httpMethod, requestPath)
                                .retrieve()
                                .bodyToMono(MAP_TYPE)
                                .doOnError(e -> log.warn("Failed to check maker-checker config for {} {}: {}",
                                                httpMethod, requestPath, e.getMessage()))
                                .onErrorResume(e -> Mono.empty());
        }

        /**
         * Create a new approval request in the maker-checker-service.
         */
        public Mono<Map<String, Object>> createApproval(Map<String, Object> payload) {
                return webClientBuilder.build()
                                .post()
                                .uri(makerCheckerUrl + "/api/maker-checker/configs/approval-requests")
                                .bodyValue(payload)
                                .retrieve()
                                .bodyToMono(MAP_TYPE)
                                .doOnError(e -> log.error("Failed to create approval request: {}", e.getMessage()));
        }
}
