package com.cas.gateway.filter;

import com.cas.gateway.service.ScopeEnforcementService;
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
    }

    private static class ScopeEnforcementFilter implements GatewayFilter, Ordered {
        private final ScopeEnforcementService scopeEnforcementService;

        ScopeEnforcementFilter(ScopeEnforcementService scopeEnforcementService) {
            this.scopeEnforcementService = scopeEnforcementService;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getURI().getPath();

            Set<String> requiredScopes = scopeEnforcementService.getRequiredScopes(method, path);

            if (requiredScopes.isEmpty()) {
                return chain.filter(exchange);
            }

            Set<String> userScopes = exchange.getAttribute("user_scopes");
            if (userScopes == null) {
                log.warn("No user scopes found in exchange attributes for {} {}", method, path);
                return forbidden(exchange, "Access denied - no scopes available");
            }

            if (!scopeEnforcementService.hasRequiredScopes(userScopes, requiredScopes)) {
                log.warn("Insufficient scopes for {} {}. Required: {}, Has: {}",
                        method, path, requiredScopes, userScopes);
                return forbidden(exchange,
                        "Access denied - insufficient scopes. Required: " + requiredScopes);
            }

            return chain.filter(exchange);
        }

        private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = String.format(
                    "{\"error\":\"insufficient_scope\",\"error_description\":\"%s\"}",
                    message.replace("\"", "\\\""));

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }

        @Override
        public int getOrder() {
            return -50;
        }
    }
}
