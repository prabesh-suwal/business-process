package com.lms.gateway.service;

import com.lms.gateway.config.LmsGatewayProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtValidationService {

    private final LmsGatewayProperties gatewayProperties;
    private final WebClient.Builder webClientBuilder;

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile long lastKeyFetch = 0;
    private static final long KEY_CACHE_TTL = 3600000; // 1 hour

    @PostConstruct
    public void init() {
        refreshKeys().subscribe(
                success -> log.info("JWK keys loaded successfully"),
                error -> log.warn("Failed to load JWK keys on startup: {}", error.getMessage()));
    }

    public Mono<Claims> validateToken(String token) {
        return Mono.defer(() -> {
            try {
                // Get key ID from token header
                String keyId = getKeyIdFromToken(token);
                PublicKey key = keyCache.get(keyId);

                if (key == null || isKeyStale()) {
                    return refreshKeys().then(Mono.defer(() -> parseToken(token)));
                }

                return parseToken(token);
            } catch (Exception e) {
                return Mono.error(new JwtException("Token validation failed: " + e.getMessage()));
            }
        });
    }

    private Mono<Claims> parseToken(String token) {
        try {
            String keyId = getKeyIdFromToken(token);
            PublicKey key = keyCache.get(keyId);

            if (key == null) {
                return Mono.error(new JwtException("Unknown key ID: " + keyId));
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(gatewayProperties.getJwt().getIssuer())
                    .clockSkewSeconds(60) // Allow 60 seconds clock skew tolerance
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Validate audience
            Set<String> tokenAudiences = claims.getAudience();
            Set<String> allowedAudiences = gatewayProperties.getAudiences();

            if (tokenAudiences == null ||
                    Collections.disjoint(tokenAudiences, allowedAudiences)) {
                return Mono.error(new JwtException(
                        "Invalid audience. Allowed: " + allowedAudiences));
            }

            return Mono.just(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired! Expiry: {}, Now: {}", e.getClaims().getExpiration(), new java.util.Date());
            return Mono.error(new JwtException("Token expired"));
        } catch (SignatureException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            return Mono.error(new JwtException("Invalid signature"));
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return Mono.error(new JwtException("Malformed token"));
        } catch (JwtException e) {
            log.warn("JWT error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    private String getKeyIdFromToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new MalformedJwtException("Invalid token format");
        }

        byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
        String headerJson = new String(headerBytes);

        // Simple JSON parsing for kid
        int kidIndex = headerJson.indexOf("\"kid\"");
        if (kidIndex == -1) {
            throw new MalformedJwtException("No key ID in token");
        }

        int colonIndex = headerJson.indexOf(":", kidIndex);
        int startQuote = headerJson.indexOf("\"", colonIndex);
        int endQuote = headerJson.indexOf("\"", startQuote + 1);

        return headerJson.substring(startQuote + 1, endQuote);
    }

    private boolean isKeyStale() {
        return System.currentTimeMillis() - lastKeyFetch > KEY_CACHE_TTL;
    }

    private void validateAudience(Claims claims) {
        Set<String> tokenAud = claims.getAudience();
        Set<String> allowedAud = gatewayProperties.getAudiences();

        if (tokenAud == null || Collections.disjoint(tokenAud, allowedAud)) {
            throw new JwtException("Invalid audience. Allowed: " + allowedAud);
        }
    }

    public Mono<Void> refreshKeys() {
        return webClientBuilder.build()
                .get()
                .uri(gatewayProperties.getJwt().getJwksUri())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(jwks -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
                    if (keys != null) {
                        for (Map<String, Object> keyData : keys) {
                            try {
                                String kid = (String) keyData.get("kid");
                                String n = (String) keyData.get("n");
                                String e = (String) keyData.get("e");

                                PublicKey publicKey = createRsaPublicKey(n, e);
                                keyCache.put(kid, publicKey);
                                log.debug("Cached key: {}", kid);
                            } catch (Exception ex) {
                                log.warn("Failed to parse JWK key", ex);
                            }
                        }
                        lastKeyFetch = System.currentTimeMillis();
                    }
                })
                .doOnError(error -> log.error("Failed to fetch JWKS: {}", error.getMessage()))
                .then();
    }

    private PublicKey createRsaPublicKey(String nBase64, String eBase64) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(nBase64);
        byte[] eBytes = Base64.getUrlDecoder().decode(eBase64);

        BigInteger n = new BigInteger(1, nBytes);
        BigInteger e = new BigInteger(1, eBytes);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public Set<String> extractScopes(Claims claims) {
        Set<String> scopes = new HashSet<>();
        String type = claims.get("type", String.class);

        if ("SERVICE".equals(type)) {
            @SuppressWarnings("unchecked")
            List<String> s = claims.get("scopes", List.class);
            if (s != null)
                scopes.addAll(s);
            return scopes;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> products = claims.get("products", Map.class);
        if (products == null)
            return scopes;

        for (String code : gatewayProperties.getProductCodes()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> p = (Map<String, Object>) products.get(code);
            if (p != null) {
                @SuppressWarnings("unchecked")
                List<String> s = (List<String>) p.get("scopes");
                if (s != null)
                    scopes.addAll(s);
            }
        }
        return scopes;
    }

    public Map<String, Map<String, Object>> extractAllowedProducts(Claims claims) {
        Map<String, Object> products = claims.get("products", Map.class);
        if (products == null)
            return Map.of();

        Map<String, Map<String, Object>> result = new HashMap<>();
        for (String code : gatewayProperties.getProductCodes()) {
            Object p = products.get(code);
            if (p instanceof Map<?, ?> map) {
                result.put(code, (Map<String, Object>) map);
            }
        }
        return result;
    }
}
