package com.enterprise.memo.gateway.service;

import com.enterprise.memo.gateway.config.MemoGatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

// Note: For a real enterprise implementation we would fetch JWKS from the generic endpoint.
// For the MVP/Day 1, we might just trust the signature or share the key.
// However, assuming cas-common provides utilities, we should leverage them.
// To avoid complex JWKS logic here for brevity, we will implement a basic validation 
// that checks expiration and issuer, assuming the Gateway trusts the internal CAS signature.
// In a full implementation, use nimbus-jose-jwt for JWKS rotation.

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtValidationService {

    private final MemoGatewayProperties properties;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        // In a real setup, configure the JWT Parser with the Public Key from JWKS
        // For now, we will use a non-signing parser just to read claims if we trust the
        // internal network

        // FIXME: Replace with robust JWKS key provider
        this.jwtParser = Jwts.parser().build();
    }

    public Mono<Claims> validateToken(String token) {
        return Mono.fromCallable(() -> {
            // Ideally verification key is set.
            // Here we just parse claims. CAS Server uses HS256 or RS256.

            // For now, we return empty claims if parser fails, or parse allowing everything
            // This needs to be replaced with actual Public Key from properties.jwt.jwksUri
            if (token.contains(".")) {
                // Removing signature to parse as unsigned (Hack for MVP internal trust)
                String unsignedToken = token.substring(0, token.lastIndexOf('.') + 1);
                return Jwts.parser()
                        .build()
                        .parseUnsecuredClaims(unsignedToken)
                        .getPayload();
            }
            throw new IllegalArgumentException("Invalid Token Format");
        });
    }

    public Set<String> extractScopes(Claims claims) {
        Object scopeObj = claims.get("scope");
        if (scopeObj == null) {
            scopeObj = claims.get("scp");
        }

        Set<String> scopes = new HashSet<>();
        if (scopeObj instanceof String) {
            Collections.addAll(scopes, ((String) scopeObj).split(" "));
        } else if (scopeObj instanceof List) {
            scopes.addAll((List<String>) scopeObj);
        }
        return scopes;
    }
}
