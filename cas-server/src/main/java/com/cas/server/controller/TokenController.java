package com.cas.server.controller;

import com.cas.common.dto.ErrorResponse;
import com.cas.common.dto.TokenResponse;
import com.cas.common.exception.InvalidCredentialsException;
import com.cas.server.domain.session.CasSession;
import com.cas.server.domain.user.User;
import com.cas.server.repository.UserRepository;
import com.cas.server.service.AuthenticationService;
import com.cas.server.service.SessionService;
import com.cas.server.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class TokenController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    // Temporary storage for authorization codes (in production, use Redis)
    private final Map<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();

    /**
     * OAuth2 Authorization Endpoint.
     * If user has a valid SSO session, auto-approve and redirect with code.
     * If no session, redirect to login page.
     */
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "product_code", required = false, defaultValue = "LMS") String productCode,
            HttpServletRequest request) {

        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.oauth2Error("unsupported_response_type", "Only 'code' is supported"));
        }

        // Check for existing SSO session
        Optional<CasSession> sessionOpt = sessionService.validateSession(request);

        if (sessionOpt.isPresent()) {
            // SSO: User has a valid session, auto-approve
            CasSession session = sessionOpt.get();
            log.info("SSO auto-approve for user {} requesting {}", session.getUsername(), productCode);

            // Generate authorization code
            String code = generateAuthorizationCode();
            authorizationCodes.put(code, new AuthorizationCode(
                    session.getUserId(),
                    clientId,
                    redirectUri,
                    productCode,
                    scope,
                    codeChallenge,
                    codeChallengeMethod,
                    System.currentTimeMillis() + 600000 // 10 min expiry
            ));

            // Redirect back with code
            String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("code", code)
                    .queryParamIfPresent("state", Optional.ofNullable(state))
                    .build().toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        // No session - redirect to login page with original params
        String loginUrl = UriComponentsBuilder.fromPath("/login")
                .queryParam("response_type", responseType)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("product_code", productCode)
                .queryParamIfPresent("scope", Optional.ofNullable(scope))
                .queryParamIfPresent("state", Optional.ofNullable(state))
                .queryParamIfPresent("code_challenge", Optional.ofNullable(codeChallenge))
                .queryParamIfPresent("code_challenge_method", Optional.ofNullable(codeChallengeMethod))
                .build().toUriString();

        log.debug("No SSO session, redirecting to login: {}", loginUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(loginUrl))
                .build();
    }

    /**
     * Exchange authorization code for tokens.
     */
    @PostMapping("/token")
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "client_id", required = false) String clientIdParam,
            @RequestParam(value = "client_secret", required = false) String clientSecretParam,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "product_code", required = false, defaultValue = "LMS") String productCode,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        try {
            return switch (grantType) {
                case "authorization_code" -> handleAuthorizationCode(code, redirectUri, codeVerifier, clientIdParam);
                case "client_credentials" -> handleClientCredentials(
                        authHeader, clientIdParam, clientSecretParam, productCode, ipAddress);
                case "refresh_token" -> handleRefreshToken(refreshToken, productCode);
                default -> ResponseEntity.badRequest()
                        .body(ErrorResponse.oauth2Error("unsupported_grant_type",
                                "Grant type not supported: " + grantType));
            };
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.oauth2Error("invalid_client", e.getMessage()));
        }
    }

    /**
     * Handle authorization code exchange.
     */
    private ResponseEntity<?> handleAuthorizationCode(String code, String redirectUri,
            String codeVerifier, String clientId) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_request", "Authorization code required"));
        }

        AuthorizationCode authCode = authorizationCodes.remove(code);
        if (authCode == null || authCode.isExpired()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_grant", "Authorization code expired or invalid"));
        }

        // Validate redirect URI
        if (!authCode.redirectUri.equals(redirectUri)) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_grant", "Redirect URI mismatch"));
        }

        // TODO: Validate PKCE code_verifier if code_challenge was provided

        // Get user and generate tokens
        User user = userRepository.findById(authCode.userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_grant", "User not found"));
        }

        String accessToken = tokenService.generateAccessToken(user, authCode.productCode);
        String newRefreshToken = tokenService.generateRefreshToken(user, authCode.productCode);
        String scopes = String.join(" ", user.getPermissionsForProduct(authCode.productCode));

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(300L)
                .scope(scopes)
                .build();

        log.info("Exchanged authorization code for tokens, user: {}, product: {}",
                user.getUsername(), authCode.productCode);
        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 token revocation endpoint.
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        authenticationService.logout(token, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleClientCredentials(
            String authHeader, String clientIdParam, String clientSecretParam,
            String productCode, String ipAddress) {

        String clientId;
        String clientSecret;

        // Try Basic auth first
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String credentials = new String(
                    Base64.getDecoder().decode(authHeader.substring(6)),
                    StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.oauth2Error("invalid_request", "Invalid Basic auth header"));
            }
            clientId = parts[0];
            clientSecret = parts[1];
        } else if (clientIdParam != null && clientSecretParam != null) {
            clientId = clientIdParam;
            clientSecret = clientSecretParam;
        } else {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_request",
                            "Client credentials required"));
        }

        TokenResponse response = authenticationService.clientCredentialsAuth(
                clientId, clientSecret, productCode, ipAddress);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleRefreshToken(String refreshToken, String productCode) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_request", "Refresh token required"));
        }

        TokenResponse response = authenticationService.refreshToken(refreshToken, productCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle Resource Owner Password Credentials (ROPC) grant type.
     * This allows direct username/password authentication for trusted first-party
     * apps. Also creates an SSO session for cross-product single sign-on.
     */
    private ResponseEntity<?> handlePasswordGrant(String username, String password,
            String productCode, String ipAddress, String userAgent, HttpServletResponse response) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.oauth2Error("invalid_request", "Username and password required"));
        }

        try {
            com.cas.common.dto.LoginRequest loginRequest = com.cas.common.dto.LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .productCode(productCode)
                    .build();

            com.cas.common.dto.LoginResponse loginResponse = authenticationService.login(
                    loginRequest, ipAddress, userAgent);

            // Create SSO session for cross-product single sign-on
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                sessionService.createSession(user, ipAddress, userAgent, response);
                log.info("SSO session created for user {} via password grant", username);
            }

            log.info("Password grant: tokens issued for user {}", username);
            // Return just the token response for OAuth compatibility
            return ResponseEntity.ok(loginResponse.getTokens());
        } catch (InvalidCredentialsException e) {
            log.warn("Password grant failed for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.oauth2Error("invalid_grant", "Invalid username or password"));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Generate a secure random authorization code.
     */
    private String generateAuthorizationCode() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Authorization code holder for OAuth2 authorization code flow.
     */
    private record AuthorizationCode(
            UUID userId,
            String clientId,
            String redirectUri,
            String productCode,
            String scope,
            String codeChallenge,
            String codeChallengeMethod,
            long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
