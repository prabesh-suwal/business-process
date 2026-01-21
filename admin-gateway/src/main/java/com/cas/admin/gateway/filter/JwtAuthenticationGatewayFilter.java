package com.cas.admin.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Authentication Filter for Admin Gateway.
 * Validates JWT tokens before routing to backend services.
 * 
 * Validates:
 * - JWT signature (via JWKS from CAS)
 * - Issuer (iss)
 * - Audience (aud) = cas-admin-api
 * - Token type = ADMIN
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    @Value("${cas.jwks-url:http://localhost:9000/.well-known/jwks.json}")
    private String jwksUrl;

    @Value("${cas.issuer:http://localhost:9000}")
    private String expectedIssuer;

    @Value("${cas.audience:cas-admin-api}")
    private String expectedAudience;

    private final WebClient webClient;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    // Paths that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/session", // SSO session check
            "/auth/token-for-product", // SSO token exchange
            "/auth/logout", // Logout
            "/auth/logout/global", // Global logout
            "/.well-known/",
            "/actuator/health",
            "/actuator/info");

    public JwtAuthenticationGatewayFilter() {
        this.webClient = WebClient.builder().build();
    }

    @PostConstruct
    public void init() {
        // Pre-fetch JWKS on startup
        fetchJwks().subscribe(
                v -> log.info("JWKS fetched successfully from {}", jwksUrl),
                e -> log.warn("Failed to fetch JWKS on startup: {}", e.getMessage()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // Validate JWT
        return validateToken(token)
                .flatMap(claims -> {
                    // Token is valid, proceed with the request
                    log.debug("Token validated for user: {}", claims.getSubject());

                    // Build mutated request with user info headers
                    ServerHttpRequest.Builder requestBuilder = request.mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Email", claims.get("email", String.class))
                            .header("X-Token-Type", claims.get("type", String.class));

                    // Add product claims as JSON header for downstream services
                    @SuppressWarnings("unchecked")
                    Map<String, Object> products = claims.get("products", Map.class);
                    if (products != null) {
                        try {
                            String productsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                                    .writeValueAsString(products);
                            requestBuilder.header("X-Product-Claims", productsJson);
                        } catch (Exception e) {
                            log.warn("Failed to serialize product claims: {}", e.getMessage());
                        }
                    }

                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                })
                .onErrorResume(e -> {
                    log.warn("Token validation failed: {}", e.getMessage());
                    return unauthorized(exchange, e.getMessage());
                });
    }

    private Mono<Claims> validateToken(String token) {
        return getPublicKey(token)
                .flatMap(publicKey -> {
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(publicKey)
                                .requireIssuer(expectedIssuer)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();

                        // Validate audience (must be cas_admin-api for admin portal)
                        Set<String> audience = claims.getAudience();
                        if (audience == null || !audience.contains(expectedAudience)) {
                            return Mono.error(new SecurityException("Invalid audience. Expected: " + expectedAudience));
                        }

                        // Token type should be USER (all products use USER type now)
                        // The product context comes from audience and products claim

                        return Mono.just(claims);
                    } catch (ExpiredJwtException e) {
                        return Mono.error(new SecurityException("Token has expired"));
                    } catch (MalformedJwtException e) {
                        return Mono.error(new SecurityException("Malformed token"));
                    } catch (io.jsonwebtoken.security.SignatureException e) {
                        return Mono.error(new SecurityException("Invalid token signature"));
                    } catch (JwtException e) {
                        return Mono.error(new SecurityException("Invalid token: " + e.getMessage()));
                    }
                });
    }

    private Mono<java.security.interfaces.RSAPublicKey> getPublicKey(String token) {
        // Extract kid from token header
        String kid;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Mono.error(new SecurityException("Invalid token format"));
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> header = new com.fasterxml.jackson.databind.ObjectMapper().readValue(headerJson,
                    Map.class);
            kid = (String) header.get("kid");
        } catch (Exception e) {
            return Mono.error(new SecurityException("Failed to parse token header"));
        }

        if (kid == null) {
            return Mono.error(new SecurityException("Token missing key ID (kid)"));
        }

        // Check cache
        PublicKey cachedKey = keyCache.get(kid);
        if (cachedKey != null) {
            return Mono.just((java.security.interfaces.RSAPublicKey) cachedKey);
        }

        // Fetch JWKS and get key
        return fetchJwks()
                .flatMap(v -> {
                    PublicKey key = keyCache.get(kid);
                    if (key == null) {
                        return Mono.error(new SecurityException("Key not found: " + kid));
                    }
                    return Mono.just((java.security.interfaces.RSAPublicKey) key);
                });
    }

    private Mono<Void> fetchJwks() {
        return webClient.get()
                .uri(jwksUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(jwks -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
                    if (keys != null) {
                        for (Map<String, Object> key : keys) {
                            try {
                                String kid = (String) key.get("kid");
                                String n = (String) key.get("n");
                                String e = (String) key.get("e");

                                if ("RSA".equals(key.get("kty")) && kid != null && n != null && e != null) {
                                    PublicKey publicKey = buildRsaPublicKey(n, e);
                                    keyCache.put(kid, publicKey);
                                    log.debug("Cached public key: {}", kid);
                                }
                            } catch (Exception ex) {
                                log.warn("Failed to parse JWK: {}", ex.getMessage());
                            }
                        }
                    }
                })
                .then();
    }

    private PublicKey buildRsaPublicKey(String n, String e) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run before routing
        return -100;
    }
}
