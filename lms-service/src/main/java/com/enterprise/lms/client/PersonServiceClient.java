package com.enterprise.lms.client;

import com.cas.common.webclient.InternalWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * Client for person-service API calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonServiceClient {

    @InternalWebClient
    private final WebClient.Builder webClientBuilder;

    @Value("${services.person-service.url:http://localhost:9007}")
    private String personServiceUrl;

    /**
     * Get person by ID.
     */
    public Map<String, Object> getPerson(UUID personId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(personServiceUrl + "/api/persons/" + personId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to get person {}: {}", personId, e.getMessage());
            return null;
        }
    }

    /**
     * Get person by citizenship number.
     */
    public Map<String, Object> getPersonByCitizenship(String citizenshipNumber) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(personServiceUrl + "/api/persons/by-identifier?type=CITIZENSHIP&value=" + citizenshipNumber)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to get person by citizenship {}: {}", citizenshipNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Search persons by name or phone.
     */
    public Object searchPersons(String query) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(personServiceUrl + "/api/persons/search?q=" + query)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to search persons: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Add a role to a person (e.g., borrower on a loan).
     */
    public Map<String, Object> addRole(UUID personId, String roleType, String productCode,
            UUID productId, String entityType, UUID entityId) {
        try {
            Map<String, Object> request = Map.of(
                    "roleType", roleType,
                    "productCode", productCode,
                    "productId", productId != null ? productId.toString() : "",
                    "entityType", entityType,
                    "entityId", entityId.toString());

            return webClientBuilder.build()
                    .post()
                    .uri(personServiceUrl + "/api/persons/" + personId + "/roles")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to add role for person {}: {}", personId, e.getMessage());
            return null;
        }
    }

    /**
     * Get 360° view for a person.
     */
    public Map<String, Object> get360View(UUID personId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(personServiceUrl + "/api/persons/" + personId + "/360-view")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to get 360° view for person {}: {}", personId, e.getMessage());
            return null;
        }
    }
}
