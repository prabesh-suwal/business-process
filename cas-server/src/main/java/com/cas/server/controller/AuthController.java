package com.cas.server.controller;

import com.cas.common.dto.LoginRequest;
import com.cas.common.dto.LoginResponse;
import com.cas.server.domain.session.CasSession;
import com.cas.server.domain.user.User;
import com.cas.server.repository.UserRepository;
import com.cas.server.service.AuthenticationService;
import com.cas.server.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    /**
     * Login endpoint - creates SSO session if successful.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authenticationService.login(request, ipAddress, userAgent);

        // Create SSO session for the user
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user != null) {
            CasSession session = sessionService.createSession(user, ipAddress, userAgent, httpResponse);
            log.info("SSO session created for user {} with session ID {}", user.getUsername(), session.getId());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint - destroys refresh token.
     * For product-specific logout, only the token is revoked.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (refreshToken != null) {
            authenticationService.logout(refreshToken, ipAddress, userAgent);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Global logout - destroys SSO session (affects all products).
     */
    @PostMapping("/logout/global")
    public ResponseEntity<Map<String, String>> globalLogout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        sessionService.destroySession(httpRequest, httpResponse);
        log.info("Global SSO logout performed");
        return ResponseEntity.ok(Map.of("message", "Logged out from all products"));
    }

    /**
     * Refresh token endpoint.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            var response = authenticationService.refreshToken(
                    request.getRefreshToken(),
                    request.getProductCode() != null ? request.getProductCode() : "CAS_ADMIN");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                    "error", "invalid_grant",
                    "message", e.getMessage()));
        }
    }

    @lombok.Data
    public static class RefreshRequest {
        private String refreshToken;
        private String productCode;
    }

    /**
     * Check if user has an active SSO session.
     */
    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpServletRequest httpRequest) {
        Optional<CasSession> sessionOpt = sessionService.validateSession(httpRequest);
        if (sessionOpt.isPresent()) {
            CasSession session = sessionOpt.get();
            return ResponseEntity.ok(Map.of(
                    "active", true,
                    "userId", session.getUserId().toString(),
                    "username", session.getUsername(),
                    "email", session.getEmail(),
                    "loginTime", session.getLoginTime().toString()));
        }
        return ResponseEntity.ok(Map.of("active", false));
    }

    /**
     * Get product-specific tokens when SSO session exists.
     * This enables cross-product SSO: login once, get tokens for any product.
     */
    @PostMapping("/token-for-product")
    public ResponseEntity<?> tokenForProduct(
            @RequestBody TokenForProductRequest request,
            HttpServletRequest httpRequest) {

        // Validate SSO session
        Optional<CasSession> sessionOpt = sessionService.validateSession(httpRequest);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "no_session",
                    "message", "No active SSO session"));
        }

        CasSession session = sessionOpt.get();

        // Get user and generate product-specific tokens
        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "user_not_found",
                    "message", "User not found"));
        }

        String productCode = request.getProductCode();
        if (productCode == null || productCode.isBlank()) {
            productCode = "LMS"; // Default
        }

        // Check if user has any roles for this product
        if (user.getRolesForProduct(productCode).isEmpty()) {
            log.warn("SSO denied: user {} has no roles for product {}",
                    user.getUsername(), productCode);
            return ResponseEntity.status(403).body(Map.of(
                    "error", "no_product_access",
                    "message", "You do not have access to this product"));
        }

        // Use authentication service to generate tokens
        com.cas.common.dto.TokenResponse tokens = authenticationService.generateTokensForUser(
                user, productCode);

        log.info("SSO token exchange: user {} got tokens for product {}",
                user.getUsername(), productCode);

        return ResponseEntity.ok(LoginResponse.builder()
                .tokens(tokens)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build())
                .build());
    }

    @lombok.Data
    public static class TokenForProductRequest {
        private String productCode;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
