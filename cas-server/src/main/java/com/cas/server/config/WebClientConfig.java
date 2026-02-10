package com.cas.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for HTTP clients used by the application.
 */
@Configuration
public class WebClientConfig {

    /**
     * Provides a WebClient.Builder bean required by HttpAuditEventPublisher.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
