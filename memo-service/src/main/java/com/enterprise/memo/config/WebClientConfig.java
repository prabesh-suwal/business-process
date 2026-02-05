package com.enterprise.memo.config;

import com.cas.common.webclient.UserContextWebClientFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration that automatically propagates UserContext headers
 * for service-to-service calls.
 */
@Configuration
public class WebClientConfig {

    @Value("${spring.application.name:memo-service}")
    private String serviceName;

    /**
     * Creates a user context propagating filter.
     */
    @Bean
    public UserContextWebClientFilter userContextWebClientFilter() {
        return new UserContextWebClientFilter(serviceName);
    }

    /**
     * Primary WebClient.Builder that automatically propagates UserContext headers.
     * All WebClient calls in this service will use this builder.
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(UserContextWebClientFilter filter) {
        return WebClient.builder().filter(filter);
    }
}
