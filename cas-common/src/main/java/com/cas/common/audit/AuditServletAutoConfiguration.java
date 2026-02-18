package com.cas.common.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Servlet-specific audit auto-configuration.
 * 
 * Only activates in servlet-based web applications (e.g., cas-server,
 * memo-service).
 * Never loads in reactive environments (e.g., gateway-product, admin-gateway).
 * 
 * Provides:
 * - {@link CorrelationIdFilter} — MDC-based correlation ID propagation
 * - {@link HttpAuditEventPublisher} — HTTP publisher for {@link AuditEvent}s
 * - {@link AuditAspect} — AOP aspect for {@link Auditable} annotation
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditServletAutoConfiguration {

    /**
     * Servlet filter for correlation ID propagation via MDC.
     * Runs at highest precedence to ensure correlation IDs are available
     * for all downstream filters and handlers.
     */
    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * HTTP-based audit event publisher using WebClient.
     * Publishes structured audit events to the centralized audit service.
     */
    @Bean
    @ConditionalOnMissingBean(AuditEventPublisher.class)
    @ConditionalOnClass(WebClient.class)
    public HttpAuditEventPublisher httpAuditEventPublisher(
            WebClient.Builder webClientBuilder,
            AuditProperties properties) {
        return new HttpAuditEventPublisher(webClientBuilder, properties);
    }

    /**
     * AOP aspect for the @Auditable annotation.
     * Intercepts annotated methods and publishes audit events automatically.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuditEventPublisher.class)
    public AuditAspect auditAspect(
            AuditEventPublisher publisher,
            AuditProperties properties) {
        return new AuditAspect(publisher, properties);
    }
}
