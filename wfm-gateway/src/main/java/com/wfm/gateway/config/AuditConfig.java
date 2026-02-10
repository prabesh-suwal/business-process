package com.wfm.gateway.config;

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

    private final GatewayProperties gatewayProperties;
    private final AuditProperties auditProperties;

    public AuditConfig(GatewayProperties gatewayProperties, AuditProperties auditProperties) {
        this.gatewayProperties = gatewayProperties;
        this.auditProperties = auditProperties;
    }

    @Bean
    public GatewayApiLogProperties gatewayApiLogProperties() {
        return new GatewayApiLogProperties();
    }

    @Bean
    public GatewayApiLogFilter gatewayApiLogFilter(GatewayApiLogProperties gatewayApiLogProperties) {
        return new GatewayApiLogFilter(
                gatewayProperties.getProductCode(),
                auditProperties.getServiceUrl(),
                gatewayApiLogProperties);
    }
}
