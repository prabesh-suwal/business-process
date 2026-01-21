package com.wfm.gateway.filter;

import com.wfm.gateway.service.ScopeEnforcementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
public class ScopeEnforcementGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ScopeEnforcementGatewayFilterFactory.Config> {

    private final ScopeEnforcementService scopeEnforcementService;

    public ScopeEnforcementGatewayFilterFactory(ScopeEnforcementService scopeEnforcementService) {
        super(Config.class);
        this.scopeEnforcementService = scopeEnforcementService;
    }

    @Override
    public String name() {
        return "ScopeEnforcement";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new ScopeEnforcementFilter(scopeEnforcementService);
    }

    public static class Config {
        // Configuration properties if needed
    }

    private static class ScopeEnforcementFilter implements GatewayFilter, Ordered {

        private final ScopeEnforcementService scopeEnforcementService;

        ScopeEnforcementFilter(ScopeEnforcementService scopeEnforcementService) {
            this.scopeEnforcementService = scopeEnforcementService;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // Get user scopes from JWT filter
            @SuppressWarnings("unchecked")
            Set<String> userScopes = (Set<String>) exchange.getAttributes()
                    .getOrDefault("user_scopes", Collections.emptySet());

            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().value();

            // Get required scopes for this endpoint
            Set<String> requiredScopes = scopeEnforcementService.getRequiredScopes(method, path);

            if (requiredScopes.isEmpty()) {
                // No scope requirement defined - allow by default (or deny based on policy)
                log.debug("No scope requirement for {} {} - allowing", method, path);
                return chain.filter(exchange);
            }

            // Check if user has required scopes
            if (!scopeEnforcementService.hasRequiredScopes(userScopes, requiredScopes)) {
                log.warn("Insufficient scopes for {} {}. Required: {}, Has: {}",
                        method, path, requiredScopes, userScopes);
                return forbidden(exchange, requiredScopes);
            }

            log.debug("Scope check passed for {} {}", method, path);
            return chain.filter(exchange);
        }

        private Mono<Void> forbidden(ServerWebExchange exchange, Set<String> requiredScopes) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = String.format(
                    "{\"error\":\"insufficient_scope\",\"error_description\":\"Required scopes: %s\"}",
                    String.join(", ", requiredScopes));

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }

        @Override
        public int getOrder() {
            return -50; // Run after JWT authentication
        }
    }
}
