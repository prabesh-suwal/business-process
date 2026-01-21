package com.enterprise.organization.config;

import com.cas.common.policy.PolicyClient;
import com.cas.common.policy.PolicyInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for policy enforcement.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${policy.engine.url:http://localhost:9001}")
    private String policyEngineUrl;

    @Bean
    public PolicyClient policyClient() {
        return new PolicyClient(policyEngineUrl);
    }

    @Bean
    public PolicyInterceptor policyInterceptor() {
        return new PolicyInterceptor(policyClient(), "organization-service");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(policyInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**");
    }
}
