package com.cas.server.controller;

import com.cas.common.dto.RawResponse;
import com.cas.server.domain.user.User;
import com.cas.server.repository.UserRepository;
import com.cas.server.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal endpoint for service-to-service token generation.
 * Used by maker-checker-service to obtain a JWT for the original maker
 * when executing an approved request.
 *
 * Secured via a shared internal service token — NOT exposed through the
 * gateway.
 */
@Slf4j
@RawResponse
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalTokenController {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    /**
     * Generate a short-lived access token for a specific user.
     * Called by maker-checker-service before executing an approved request.
     */
    @PostMapping("/token/generate")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token) {

        if (!internalServiceToken.equals(token)) {
            log.warn("Rejected internal token generation request — invalid token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid service token"));
        }

        String userId = request.get("userId");
        String productCode = request.get("productCode");

        if (userId == null || productCode == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId and productCode are required"));
        }

        try {
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElse(null);

            if (user == null) {
                log.warn("Internal token generation failed: user not found: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + userId));
            }

            String accessToken = tokenService.generateAccessToken(user, productCode);

            log.info("Generated internal access token for user {} product {}", userId, productCode);

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "userId", userId,
                    "productCode", productCode));

        } catch (Exception e) {
            log.error("Internal token generation failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token generation failed: " + e.getMessage()));
        }
    }
}
