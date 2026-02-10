package com.cas.common.logging;

import com.cas.common.logging.activity.ActivityLogger;
import com.cas.common.logging.activity.ActivityLogPublisher;
import com.cas.common.logging.audit.AuditLogger;
import com.cas.common.logging.audit.AuditLogPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Auto-configuration for the three-tier logging system.
 * Provides beans for AuditLogger and ActivityLogger.
 * 
 * Configuration properties:
 * - audit.service-url: URL of the audit service (default:
 * http://localhost:9009)
 * - audit.enabled: Enable/disable logging (default: true)
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "logging.system.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditLogPublisher auditLogPublisher(
            @Value("${audit.service-url:http://localhost:9009}") String auditServiceUrl,
            @Value("${audit.enabled:true}") boolean enabled) {
        return new AuditLogPublisher(auditServiceUrl, enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger(
            AuditLogPublisher publisher,
            @Value("${spring.application.name:unknown}") String serviceName) {
        return new AuditLogger(publisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityLogPublisher activityLogPublisher(
            @Value("${audit.service-url:http://localhost:9009}") String auditServiceUrl,
            @Value("${audit.enabled:true}") boolean enabled) {
        return new ActivityLogPublisher(auditServiceUrl, enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    public ActivityLogger activityLogger(ActivityLogPublisher publisher) {
        return new ActivityLogger(publisher);
    }
}
