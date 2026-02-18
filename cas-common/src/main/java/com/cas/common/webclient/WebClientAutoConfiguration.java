package com.cas.common.webclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration that provides two WebClient.Builder beans:
 * 
 * <ul>
 * <li>{@code @Primary} — plain builder for external calls (CBS, WALLET, etc.)
 * Does NOT propagate user context headers.</li>
 * <li>{@code @InternalWebClient} — builder with
 * {@link UserContextWebClientFilter}
 * for internal microservice calls (memo-service, workflow-service, etc.)
 * Automatically propagates X-User-Id, X-Roles, etc.</li>
 * </ul>
 * 
 * Services no longer need their own WebClientConfig — this is auto-configured
 * via Spring Boot's autoconfiguration mechanism.
 */
@Configuration
@ConditionalOnClass(WebClient.class)
public class WebClientAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * UserContext propagation filter for internal service-to-service calls.
     */
    @Bean
    public UserContextWebClientFilter userContextWebClientFilter() {
        return new UserContextWebClientFilter(serviceName);
    }

    /**
     * Default WebClient.Builder — plain, no filters.
     * Use this for external service calls (CBS, WALLET, etc.).
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Internal WebClient.Builder — with UserContext propagation filter.
     * Use {@code @InternalWebClient} qualifier to inject this builder.
     * 
     * <pre>
     * &#64;InternalWebClient
     * private final WebClient.Builder webClientBuilder;
     * </pre>
     */
    @Bean
    @InternalWebClient
    public WebClient.Builder internalWebClientBuilder(UserContextWebClientFilter filter) {
        return WebClient.builder().filter(filter);
    }
}
