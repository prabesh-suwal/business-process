package com.enterprise.memo.gateway.filter;

import com.enterprise.memo.gateway.config.MemoGatewayProperties;
import com.enterprise.memo.gateway.service.JwtValidationService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtValidationService jwtValidationService;
    private final MemoGatewayProperties gatewayProperties;

    public JwtAuthenticationGatewayFilterFactory(
            JwtValidationService jwtValidationService,
            MemoGatewayProperties gatewayProperties) {
        super(Config.class);
        this.jwtValidationService = jwtValidationService;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public String name() {
        return "JwtAuthentication";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new JwtAuthenticationFilter(jwtValidationService, gatewayProperties);
    }

    public static class Config {
        // Configuration properties if needed
    }

    private static class JwtAuthenticationFilter implements GatewayFilter, Ordered {

        private final JwtValidationService jwtValidationService;
        private final MemoGatewayProperties gatewayProperties;

        JwtAuthenticationFilter(JwtValidationService jwtValidationService,
                MemoGatewayProperties gatewayProperties) {
            this.jwtValidationService = jwtValidationService;
            this.gatewayProperties = gatewayProperties;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            return jwtValidationService.validateToken(token)
                    .flatMap(claims -> {
                        // Add headers for downstream services
                        ServerWebExchange mutatedExchange = addSecurityHeaders(exchange, claims);

                        // Store claims in exchange attributes for scope filter
                        mutatedExchange.getAttributes().put("jwt_claims", claims);
                        mutatedExchange.getAttributes().put("user_scopes",
                                jwtValidationService.extractScopes(claims));

                        return chain.filter(mutatedExchange);
                    })
                    .onErrorResume(error -> {
                        log.warn("JWT validation failed: {}", error.getMessage());
                        return unauthorized(exchange, error.getMessage());
                    });
        }

        private ServerWebExchange addSecurityHeaders(ServerWebExchange exchange, Claims claims) {
            String type = claims.get("type", String.class);

            ServerWebExchange.Builder builder = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Id", claims.getSubject())
                            .header("X-Token-Type", type)
                            .header("X-Token-Jti", claims.getId())
                            .header("X-Product-Code", gatewayProperties.getProductCode()));

            if ("USER".equals(type)) {
                String email = claims.get("email", String.class);
                String name = claims.get("name", String.class);

                if (email != null) {
                    builder.request(r -> r.header("X-User-Email", email));
                }
                if (name != null) {
                    builder.request(r -> r.header("X-User-Name", name));
                }

                // Extract roles for the product
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> products = claims.get("products", java.util.Map.class);
                if (products != null) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> productData = (java.util.Map<String, Object>) products
                            .get(gatewayProperties.getProductCode());
                    if (productData != null) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) productData.get("roles");
                        if (roles != null && !roles.isEmpty()) {
                            builder.request(r -> r.header("X-Roles", String.join(",", roles)));
                        }
                    }
                }
            } else if ("SERVICE".equals(type)) {
                String clientName = claims.get("client_name", String.class);
                if (clientName != null) {
                    builder.request(r -> r.header("X-Client-Name", clientName));
                }
            }

            // Add scopes header
            Set<String> scopes = jwtValidationService.extractScopes(claims);
            if (!scopes.isEmpty()) {
                builder.request(r -> r.header("X-Scopes", String.join(",", scopes)));
            }

            return builder.build();
        }

        private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE); // Explicitly remove header

            String body = String.format(
                    "{\"error\":\"invalid_token\",\"error_description\":\"%s\"}",
                    message.replace("\"", "\\\""));

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }

        @Override
        public int getOrder() {
            return -100; // Run before scope enforcement
        }
    }
}
