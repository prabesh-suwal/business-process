package com.cas.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared JWT Authentication Filter for microservices.
 * Validates JWT tokens using CAS server's JWKS endpoint.
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final String jwksUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile long lastKeyFetch = 0;
    private static final long KEY_CACHE_TTL = 3600000; // 1 hour

    public JwtAuthFilter(String jwksUrl) {
        this.jwksUrl = jwksUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = validateToken(token);
                if (claims != null) {
                    setSecurityContext(claims, request);
                }
            } catch (Exception e) {
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private Claims validateToken(String token) {
        try {
            // Parse header to get key ID
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
            String kid = (String) header.get("kid");

            if (kid == null) {
                log.debug("No key ID in token header");
                return null;
            }

            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                log.debug("Could not find public key for kid: {}", kid);
                return null;
            }

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired");
            return null;
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return null;
        }
    }

    private void setSecurityContext(Claims claims, HttpServletRequest request) {
        String userId = claims.getSubject();

        // Extract roles from product claims
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        Object productsObj = claims.get("products");
        if (productsObj instanceof Map) {
            Map<String, Object> products = (Map<String, Object>) productsObj;
            for (Object productData : products.values()) {
                if (productData instanceof Map) {
                    Object rolesObj = ((Map<String, Object>) productData).get("roles");
                    if (rolesObj instanceof List) {
                        for (Object role : (List<?>) rolesObj) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                        }
                    }
                }
            }
        }

        // Set org info in request attributes and headers for interceptor
        Object orgObj = claims.get("org");
        if (orgObj instanceof Map) {
            Map<String, Object> org = (Map<String, Object>) orgObj;
            if (org.get("branchId") != null) {
                request.setAttribute("branchId", org.get("branchId").toString());
            }
            if (org.get("departmentId") != null) {
                request.setAttribute("departmentId", org.get("departmentId").toString());
            }
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, authorities);
        auth.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PublicKey getPublicKey(String kid) {
        // Check cache
        if (keyCache.containsKey(kid) && (System.currentTimeMillis() - lastKeyFetch) < KEY_CACHE_TTL) {
            return keyCache.get(kid);
        }

        // Fetch from JWKS
        try {
            refreshKeyCache();
            return keyCache.get(kid);
        } catch (Exception e) {
            log.error("Failed to fetch JWKS: {}", e.getMessage());
            return keyCache.get(kid); // Return cached key if fetch fails
        }
    }

    private synchronized void refreshKeyCache() throws Exception {
        if ((System.currentTimeMillis() - lastKeyFetch) < 60000) {
            return; // Don't refresh more than once per minute
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("JWKS fetch failed: " + response.statusCode());
        }

        Map<String, Object> jwks = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

        for (Map<String, Object> key : keys) {
            String kid = (String) key.get("kid");
            String n = (String) key.get("n");
            String e = (String) key.get("e");

            if (kid != null && n != null && e != null) {
                PublicKey publicKey = createRSAPublicKey(n, e);
                keyCache.put(kid, publicKey);
            }
        }

        lastKeyFetch = System.currentTimeMillis();
        log.info("Refreshed JWKS cache with {} keys", keys.size());
    }

    private PublicKey createRSAPublicKey(String n, String e) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}
