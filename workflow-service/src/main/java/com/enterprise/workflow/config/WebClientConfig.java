package com.enterprise.workflow.config;

import com.cas.common.webclient.UserContextWebClientFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used by AssignmentTaskListener
 * to call memo-service webhook.
 * 
 * Automatically propagates UserContext headers for service-to-service calls.
 */
@Configuration
public class WebClientConfig {

    @Value("${spring.application.name:workflow-service}")
    private String serviceName;

    @Bean
    public UserContextWebClientFilter userContextWebClientFilter() {
        return new UserContextWebClientFilter(serviceName);
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(UserContextWebClientFilter filter) {
        return WebClient.builder().filter(filter);
    }
}
