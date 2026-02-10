package com.cas.common.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Core audit auto-configuration that loads in ALL environments (servlet and
 * reactive).
 * 
 * Provides:
 * - {@link AuditProperties} bean via {@link EnableConfigurationProperties}
 * 
 * This configuration is environment-agnostic and safe for both servlet and
 * reactive apps.
 * Servlet-specific and reactive-specific beans are registered in their
 * respective
 * auto-configuration classes.
 *
 * @see AuditServletAutoConfiguration
 * @see AuditReactiveAutoConfiguration
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditCoreAutoConfiguration {
    // AuditProperties is registered via @EnableConfigurationProperties
}
