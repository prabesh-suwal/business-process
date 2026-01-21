package com.enterprise.memo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gateway filter that propagates the Cookie header from the incoming request
 * to the backend service. This is essential for SSO - the browser sends the
 * SSO cookie to the gateway, and we need to forward it to CAS.
 */
@Slf4j
@Component
public class CookiePropagationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public CookiePropagationGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Get Cookie header from incoming request
            String cookieHeader = request.getHeaders().getFirst("Cookie");

            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                log.debug("Propagating Cookie header to backend: {}",
                        cookieHeader.substring(0, Math.min(50, cookieHeader.length())) + "...");

                // The Cookie header should already be in the request, but let's ensure it's
                // forwarded
                // Spring Cloud Gateway should forward it by default, but this filter makes it
                // explicit
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("Cookie", cookieHeader)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        };
    }
}
