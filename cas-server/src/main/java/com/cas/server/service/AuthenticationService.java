package com.cas.server.service;

import com.cas.common.dto.LoginRequest;
import com.cas.common.dto.LoginResponse;
import com.cas.common.dto.TokenResponse;
import com.cas.common.exception.AccountLockedException;
import com.cas.common.exception.InvalidCredentialsException;
import com.cas.common.exception.InvalidTokenException;
import com.cas.server.domain.audit.AuditLog;
import com.cas.server.domain.client.ApiClient;
import com.cas.server.domain.token.RefreshToken;
import com.cas.server.domain.user.User;
import com.cas.server.repository.ApiClientRepository;
import com.cas.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ApiClientRepository apiClientRepository;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user with username/password and issue tokens for a product.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", request.getUsername());
                    auditService.logLoginFailure(request.getUsername(), ipAddress, userAgent, "User not found");
                    return new InvalidCredentialsException();
                });

        // Check user status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login failed: account not active - {}", request.getUsername());
            auditService.logLoginFailure(request.getUsername(), ipAddress, userAgent, "Account not active");
            throw new AccountLockedException("Account is " + user.getStatus().name().toLowerCase());
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password - {}", request.getUsername());
            auditService.logLoginFailure(request.getUsername(), ipAddress, userAgent, "Invalid password");
            throw new InvalidCredentialsException();
        }

        // Determine product (default to first available if not specified)
        String productCode = request.getProductCode();
        if (productCode == null || productCode.isBlank()) {
            productCode = "LMS"; // Default product
        }

        // Normalize legacy "ADMIN" to "CAS_ADMIN"
        if ("ADMIN".equalsIgnoreCase(productCode)) {
            productCode = "CAS_ADMIN";
        }

        // Check if user has any roles for this product
        if (user.getRolesForProduct(productCode).isEmpty()) {
            log.warn("Login denied: user {} has no roles for product {}", user.getUsername(), productCode);
            auditService.logLoginFailure(request.getUsername(), ipAddress, userAgent,
                    "No access to product: " + productCode);
            throw new RuntimeException("You do not have access to this product");
        }

        // Generate tokens using standard flow for all products (including CAS_ADMIN)
        String accessToken = tokenService.generateAccessToken(user, productCode);
        String refreshToken = tokenService.generateRefreshToken(user, productCode);
        String scope = String.join(" ", user.getPermissionsForProduct(productCode));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Audit
        auditService.logLoginSuccess(user, productCode, ipAddress, userAgent);

        log.info("User {} logged in successfully for product {}", user.getUsername(), productCode);

        return LoginResponse.builder()
                .sessionId(null) // Session ID will be set by session service
                .tokens(TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(300L) // 5 minutes
                        .scope(scope)
                        .build())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build())
                .build();
    }

    /**
     * Generate tokens for an already-authenticated user (for SSO token exchange).
     * No password validation - assumes user identity verified via SSO session.
     */
    @Transactional
    public TokenResponse generateTokensForUser(User user, String productCode) {
        // Normalize legacy "ADMIN" to "CAS_ADMIN"
        if ("ADMIN".equalsIgnoreCase(productCode)) {
            productCode = "CAS_ADMIN";
        }

        String accessToken = tokenService.generateAccessToken(user, productCode);
        String refreshToken = tokenService.generateRefreshToken(user, productCode);
        String scope = String.join(" ", user.getPermissionsForProduct(productCode));

        log.info("Generated SSO tokens for user {} for product {}", user.getUsername(), productCode);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(300L)
                .scope(scope)
                .build();
    }

    /**
     * Client credentials flow for API clients.
     */
    @Transactional
    public TokenResponse clientCredentialsAuth(String clientId, String clientSecret,
            String productCode, String ipAddress) {
        ApiClient client = apiClientRepository.findByClientIdWithScopes(clientId)
                .orElseThrow(() -> {
                    log.warn("Client auth failed: client not found - {}", clientId);
                    return new InvalidCredentialsException("Invalid client credentials");
                });

        // Check client status
        if (!client.isActive()) {
            log.warn("Client auth failed: client not active - {}", clientId);
            throw new InvalidCredentialsException("Client is not active");
        }

        // Check IP whitelist
        if (!client.isIpAllowed(ipAddress)) {
            log.warn("Client auth failed: IP not allowed - {} from {}", clientId, ipAddress);
            throw new InvalidCredentialsException("IP address not allowed");
        }

        // Verify secret
        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            log.warn("Client auth failed: invalid secret - {}", clientId);
            throw new InvalidCredentialsException("Invalid client credentials");
        }

        // Generate token
        String accessToken = tokenService.generateAccessToken(client, productCode);
        String refreshToken = tokenService.generateRefreshToken(client, productCode);

        // Update last used
        client.setLastUsedAt(Instant.now());
        apiClientRepository.save(client);

        // Audit
        auditService.logEvent(AuditLog.EventType.TOKEN_ISSUED, AuditLog.ActorType.CLIENT,
                client.getId(), "API_CLIENT", client.getId(), productCode, ipAddress, null, null);

        log.info("Client {} authenticated successfully for product {}", clientId, productCode);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(300L)
                .scope(String.join(" ", client.getScopeCodes()))
                .build();
    }

    /**
     * Refresh token flow.
     */
    @Transactional
    public TokenResponse refreshToken(String refreshTokenValue, String productCode) {
        RefreshToken storedToken = tokenService.validateRefreshToken(refreshTokenValue);

        // Revoke old refresh token
        storedToken.revoke();

        if (storedToken.isUserToken()) {
            User user = storedToken.getUser();
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                throw new InvalidTokenException("User account is not active");
            }

            // Normalize legacy "ADMIN" to "CAS_ADMIN"
            String normalizedProductCode = "ADMIN".equalsIgnoreCase(productCode) ? "CAS_ADMIN" : productCode;

            // Standard token generation for all products
            String newAccessToken = tokenService.generateAccessToken(user, normalizedProductCode);
            String newRefreshToken = tokenService.generateRefreshToken(user, normalizedProductCode);
            String scope = String.join(" ", user.getPermissionsForProduct(normalizedProductCode));

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(300L)
                    .scope(scope)
                    .build();
        } else {
            ApiClient client = storedToken.getClient();
            if (!client.isActive()) {
                throw new InvalidTokenException("Client is not active");
            }

            String newAccessToken = tokenService.generateAccessToken(client, productCode);
            String newRefreshToken = tokenService.generateRefreshToken(client, productCode);

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(300L)
                    .scope(String.join(" ", client.getScopeCodes()))
                    .build();
        }
    }

    /**
     * Logout - revoke refresh token.
     */
    @Transactional
    public void logout(String refreshTokenValue, String ipAddress, String userAgent) {
        tokenService.revokeRefreshToken(refreshTokenValue);
        log.info("Logout completed, refresh token revoked");
    }
}
