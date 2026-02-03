package com.lms.gateway.filter;

import com.lms.gateway.config.LmsGatewayProperties;
import com.lms.gateway.service.JwtValidationService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtValidationService jwtValidationService;
    private final LmsGatewayProperties gatewayProperties;

    public JwtAuthenticationGatewayFilterFactory(
            JwtValidationService jwtValidationService,
            LmsGatewayProperties gatewayProperties) {
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
        private final LmsGatewayProperties gatewayProperties;

        JwtAuthenticationFilter(JwtValidationService jwtValidationService,
                LmsGatewayProperties gatewayProperties) {
            this.jwtValidationService = jwtValidationService;
            this.gatewayProperties = gatewayProperties;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // Allow OPTIONS requests (CORS preflight) without authentication
            if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            log.info("Processing request: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI());

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for {}", exchange.getRequest().getURI());
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            // log.debug("Token received: {}", token.substring(0, Math.min(10,
            // token.length())) + "...");

            return jwtValidationService.validateToken(token)
                    .flatMap(claims -> {
                        log.info("Token validated for user: {}", claims.getSubject());
                        // Add headers for downstream services
                        ServerWebExchange mutatedExchange = addSecurityHeaders(exchange, claims);

                        // Store claims in exchange attributes for scope filter
                        mutatedExchange.getAttributes().put("jwt_claims", claims);
                        mutatedExchange.getAttributes().put("user_scopes",
                                jwtValidationService.extractScopes(claims));

                        return chain.filter(mutatedExchange);
                    })
                    .onErrorResume(error -> {
                        log.warn("JWT validation failed for {}: {}", exchange.getRequest().getURI(),
                                error.getMessage());
                        return unauthorized(exchange, error.getMessage());
                    });
        }

        private ServerWebExchange addSecurityHeaders(ServerWebExchange exchange, Claims claims) {
            String type = claims.get("type", String.class);
            Map<String, Map<String, Object>> products = jwtValidationService.extractAllowedProducts(claims);
            Set<String> scopes = jwtValidationService.extractScopes(claims);

            // CRITICAL: All headers must be set in a SINGLE request() call
            // because ServerWebExchange.Builder is immutable - each call creates a new
            // builder
            return exchange.mutate()
                    .request((org.springframework.http.server.reactive.ServerHttpRequest.Builder r) -> {
                        // Base headers for all token types
                        r.header("X-User-Id", claims.getSubject())
                                .header("X-Token-Type", type)
                                .header("X-Token-Jti", claims.getId())
                                .header("X-Product-Code", String.join(",", products.keySet()));

                        // Extract productId from first product's claims
                        if (!products.isEmpty()) {
                            Map<String, Object> firstProduct = products.values().iterator().next();
                            Object productIdObj = firstProduct.get("productId");
                            if (productIdObj != null) {
                                r.header("X-Product-Id", productIdObj.toString());
                            }
                        }

                        // Add scopes header
                        if (!scopes.isEmpty()) {
                            r.header("X-Scopes", String.join(",", scopes));
                        }

                        // User-specific headers
                        if ("USER".equals(type)) {
                            String email = claims.get("email", String.class);
                            String name = claims.get("name", String.class);

                            if (email != null) {
                                r.header("X-User-Email", email);
                            }
                            if (name != null) {
                                r.header("X-User-Name", name);
                            }

                            // Extract roles from all products
                            Set<String> roles = new HashSet<>();
                            products.values().forEach(p -> {
                                List<String> productRoles = (List<String>) p.get("roles");
                                if (productRoles != null) {
                                    roles.addAll(productRoles);
                                }
                            });

                            if (!roles.isEmpty()) {
                                String rolesString = String.join(",", roles);
                                r.header("X-Roles", rolesString);
                                r.header("X-User-Roles", rolesString); // For workflow-service compatibility
                            }

                        } else if ("SERVICE".equals(type)) {
                            String clientName = claims.get("client_name", String.class);
                            if (clientName != null) {
                                r.header("X-Client-Name", clientName);
                            }
                        }
                    })
                    .build();
        }

        private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

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
