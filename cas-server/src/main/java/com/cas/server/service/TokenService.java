package com.cas.server.service;

import com.cas.common.exception.InvalidTokenException;
import com.cas.common.security.TokenClaims;
import com.cas.server.config.CasProperties;
import com.cas.server.config.JwtKeyConfig;
import com.cas.server.domain.client.ApiClient;
import com.cas.server.domain.role.Role;
import com.cas.server.domain.token.RefreshToken;
import com.cas.server.domain.user.User;
import com.cas.server.repository.RefreshTokenRepository;
import com.cas.server.repository.RevokedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtKeyConfig jwtKeyConfig;
    private final CasProperties casProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate access token for a user for a specific product.
     */
    public String generateAccessToken(User user, String productCode) {
        Instant now = Instant.now();
        Instant expiry = now.plus(casProperties.getJwt().getAccessTokenTtl());

        Set<Role> roles = user.getRolesForProduct(productCode);
        List<String> roleNames = roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
        Set<String> scopes = roles.stream()
                .flatMap(r -> r.getAllPermissionCodes().stream())
                .collect(Collectors.toSet());

        // Get ABAC constraints (branchIds, regionIds, maxApprovalAmount)
        Map<String, Object> constraints = user.getConstraintsForProduct(productCode);

        Map<String, Object> productClaims = new HashMap<>();
        Map<String, Object> productData = new HashMap<>();
        productData.put("roles", roleNames);
        productData.put("scopes", new ArrayList<>(scopes));
        if (!constraints.isEmpty()) {
            productData.put("constraints", constraints);
        }
        productClaims.put(productCode, productData);

        // Build organization claims for ABAC
        Map<String, Object> orgClaims = new HashMap<>();
        if (user.getBranchId() != null) {
            orgClaims.put("branchId", user.getBranchId().toString());
        }
        if (user.getDepartmentId() != null) {
            orgClaims.put("departmentId", user.getDepartmentId().toString());
        }

        var builder = Jwts.builder()
                .header()
                .keyId(jwtKeyConfig.getKeyId())
                .type("JWT")
                .and()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(casProperties.getIssuer())
                .audience().add(productCode.toLowerCase() + "-api").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "USER")
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("products", productClaims)
                .claim("ver", 1);

        // Add org claims if present
        if (!orgClaims.isEmpty()) {
            builder.claim("org", orgClaims);
        }

        return builder.signWith(jwtKeyConfig.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate access token for an API client.
     */
    public String generateAccessToken(ApiClient client, String productCode) {
        Instant now = Instant.now();
        Instant expiry = now.plus(casProperties.getJwt().getAccessTokenTtl());

        Set<String> scopes = client.getScopeCodes();

        return Jwts.builder()
                .header()
                .keyId(jwtKeyConfig.getKeyId())
                .type("JWT")
                .and()
                .id(UUID.randomUUID().toString())
                .subject(client.getId().toString())
                .issuer(casProperties.getIssuer())
                .audience().add(productCode.toLowerCase() + "-api").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "SERVICE")
                .claim("client_name", client.getName())
                .claim("scopes", new ArrayList<>(scopes))
                .claim("ver", 1)
                .signWith(jwtKeyConfig.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate a refresh token for a user.
     */
    @Transactional
    public String generateRefreshToken(User user, String productCode) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .productCode(productCode)
                .expiresAt(Instant.now().plus(casProperties.getJwt().getRefreshTokenTtl()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Generate a refresh token for an API client.
     */
    @Transactional
    public String generateRefreshToken(ApiClient client, String productCode) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .client(client)
                .productCode(productCode)
                .expiresAt(Instant.now().plus(casProperties.getJwt().getRefreshTokenTtl()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Validate and parse an access token.
     */
    public TokenClaims parseAccessToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(jwtKeyConfig.getPublicKey())
                    .requireIssuer(casProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();

            // Check if token is revoked (emergency check for high-risk ops)
            String jti = claims.getId();
            if (jti != null && revokedTokenRepository.existsByJti(UUID.fromString(jti))) {
                throw new InvalidTokenException("Token has been revoked");
            }

            return mapClaimsToTokenClaims(claims);
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired");
        } catch (SignatureException e) {
            throw new InvalidTokenException("Invalid token signature");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Malformed token");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Validate a refresh token and return the stored entity.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        return refreshTokenRepository.findValidToken(tokenHash, Instant.now())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));
    }

    /**
     * Revoke a refresh token.
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(RefreshToken::revoke);
    }

    /**
     * Revoke all refresh tokens for a user.
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    /**
     * Get JWK Set for public key distribution.
     */
    public Map<String, Object> getJwkSet() {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", jwtKeyConfig.getKeyId());
        jwk.put("n", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jwtKeyConfig.getPublicKey().getModulus().toByteArray()));
        jwk.put("e", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jwtKeyConfig.getPublicKey().getPublicExponent().toByteArray()));

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", List.of(jwk));
        return jwks;
    }

    /**
     * Get OIDC discovery document.
     */
    public Map<String, Object> getOpenIdConfiguration() {
        String issuer = casProperties.getIssuer();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/oauth/authorize");
        config.put("token_endpoint", issuer + "/oauth/token");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("revocation_endpoint", issuer + "/oauth/revoke");
        config.put("response_types_supported", List.of("code", "token"));
        config.put("grant_types_supported", List.of("authorization_code", "refresh_token", "client_credentials"));
        config.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post"));
        config.put("scopes_supported", List.of("openid", "profile", "email"));
        config.put("claims_supported", List.of("sub", "iss", "aud", "exp", "iat", "email", "name"));
        return config;
    }

    private TokenClaims mapClaimsToTokenClaims(Claims claims) {
        TokenClaims.TokenClaimsBuilder builder = TokenClaims.builder()
                .jti(claims.getId())
                .sub(claims.getSubject())
                .iss(claims.getIssuer())
                .iat(claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() / 1000 : null)
                .exp(claims.getExpiration() != null ? claims.getExpiration().getTime() / 1000 : null)
                .ver(claims.get("ver", Integer.class));

        // Extract audience
        Set<String> audience = claims.getAudience();
        if (audience != null && !audience.isEmpty()) {
            builder.aud(audience.iterator().next());
        }

        // Token type
        String type = claims.get("type", String.class);
        if ("USER".equals(type)) {
            builder.type(TokenClaims.TokenType.USER);
            builder.email(claims.get("email", String.class));
            builder.name(claims.get("name", String.class));
        } else if ("SERVICE".equals(type)) {
            builder.type(TokenClaims.TokenType.SERVICE);
            builder.clientName(claims.get("client_name", String.class));

            // Direct scopes for service tokens
            @SuppressWarnings("unchecked")
            List<String> scopesList = claims.get("scopes", List.class);
            if (scopesList != null) {
                builder.scopes(new HashSet<>(scopesList));
            }
        } else if ("ADMIN".equals(type)) {
            builder.type(TokenClaims.TokenType.ADMIN);
            builder.email(claims.get("email", String.class));
            builder.name(claims.get("name", String.class));

            // Admin scopes
            @SuppressWarnings("unchecked")
            List<String> scopesList = claims.get("scopes", List.class);
            if (scopesList != null) {
                builder.scopes(new HashSet<>(scopesList));
            }
        }

        // Extract product claims for user tokens
        @SuppressWarnings("unchecked")
        Map<String, Object> products = claims.get("products", Map.class);
        if (products != null) {
            Map<String, TokenClaims.ProductClaims> productClaimsMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : products.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> productData = (Map<String, Object>) entry.getValue();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) productData.get("roles");
                @SuppressWarnings("unchecked")
                List<String> scopes = (List<String>) productData.get("scopes");

                productClaimsMap.put(entry.getKey(), TokenClaims.ProductClaims.builder()
                        .roles(roles)
                        .scopes(scopes != null ? new HashSet<>(scopes) : new HashSet<>())
                        .build());
            }
            builder.products(productClaimsMap);
        }

        return builder.build();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
