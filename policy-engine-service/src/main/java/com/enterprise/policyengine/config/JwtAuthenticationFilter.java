package com.enterprise.policyengine.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${cas.jwks-url:http://localhost:9000/.well-known/jwks.json}")
    private String jwksUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = validateToken(token);
                String userId = claims.getSubject();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} for request: {}", userId, request.getRequestURI());
            } catch (Exception e) {
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private Claims validateToken(String token) throws Exception {
        // Get the key ID from token header
        String[] parts = token.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JsonNode header = objectMapper.readTree(headerJson);
        String kid = header.get("kid").asText();

        // Get or fetch the public key
        PublicKey publicKey = keyCache.computeIfAbsent(kid, this::fetchPublicKey);

        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PublicKey fetchPublicKey(String kid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jwks = objectMapper.readTree(response.body());

            for (JsonNode key : jwks.get("keys")) {
                if (kid.equals(key.get("kid").asText())) {
                    String n = key.get("n").asText();
                    String e = key.get("e").asText();

                    byte[] nBytes = Base64.getUrlDecoder().decode(n);
                    byte[] eBytes = Base64.getUrlDecoder().decode(e);

                    BigInteger modulus = new BigInteger(1, nBytes);
                    BigInteger exponent = new BigInteger(1, eBytes);

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    return factory.generatePublic(spec);
                }
            }
            throw new RuntimeException("Key not found: " + kid);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch public key", ex);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter evaluation endpoints (for internal service calls)
        return path.contains("/evaluate") ||
                path.contains("/swagger") ||
                path.contains("/api-docs") ||
                path.contains("/actuator");
    }
}
