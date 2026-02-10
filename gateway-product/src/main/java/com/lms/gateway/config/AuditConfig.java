package com.lms.gateway.config;

import com.cas.common.audit.AuditProperties;
import com.cas.common.logging.gateway.GatewayApiLogFilter;
import com.cas.common.logging.gateway.GatewayApiLogProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gateway API log filter.
 * Logs all API requests/responses to the audit-service for technical
 * monitoring.
 */
@Configuration
@EnableConfigurationProperties({ AuditProperties.class })
public class AuditConfig {

    @Bean
    public GatewayApiLogProperties gatewayApiLogProperties() {
        return new GatewayApiLogProperties();
    }

    @Bean
    public GatewayApiLogFilter gatewayApiLogFilter(AuditProperties auditProperties,
            GatewayApiLogProperties gatewayApiLogProperties) {
        // LMS gateway serves multiple products, use LMS as the default product code
        return new GatewayApiLogFilter(
                "LMS",
                auditProperties.getServiceUrl(),
                gatewayApiLogProperties);
    }
}
