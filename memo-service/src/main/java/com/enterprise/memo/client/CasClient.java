package com.enterprise.memo.client;

import com.cas.common.webclient.InternalWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Client for calling CAS server for user/role/organization lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasClient {

    @InternalWebClient
    private final WebClient.Builder webClientBuilder;

    @Value("${cas.service.url:http://localhost:9000}")
    private String casServiceUrl;

    /**
     * Find users by role.
     */
    public List<String> findUsersByRole(String role, String branchId) {
        log.info("Finding users with role {} at branch {}", role, branchId);

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(casServiceUrl + "/admin/users/by-role")
                            .queryParam("role", role)
                            .queryParamIfPresent("branchId", java.util.Optional.ofNullable(branchId))
                            .build())
                    .retrieve()
                    .bodyToFlux(String.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to query CAS for users: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Find users by approval authority.
     */
    public List<String> findUsersByAuthority(String role, long amount, String branchId) {
        log.info("Finding users with authority {} for amount {} at branch {}", role, amount, branchId);

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(casServiceUrl + "/admin/users/by-authority")
                            .queryParam("role", role)
                            .queryParam("amount", amount)
                            .queryParamIfPresent("branchId", java.util.Optional.ofNullable(branchId))
                            .build())
                    .retrieve()
                    .bodyToFlux(String.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to query CAS for authority: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get user details.
     */
    public Map<String, Object> getUser(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(casServiceUrl + "/admin/users/" + userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to get user {}: {}", userId, e.getMessage());
            return Map.of();
        }
    }
}
