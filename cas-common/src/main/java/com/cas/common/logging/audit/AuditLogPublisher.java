package com.cas.common.logging.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP publisher for audit log events.
 * Sends audit logs to the centralized audit-service.
 * 
 * Uses WebClient (with UserContextWebClientFilter) to automatically
 * propagate user context headers for service-to-service calls.
 * 
 * This bean is registered by
 * {@link com.cas.common.logging.LoggingAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class AuditLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPublisher.class);

    private final WebClient webClient;
    private final String auditServiceUrl;
    private final boolean enabled;

    public AuditLogPublisher(WebClient.Builder webClientBuilder, String auditServiceUrl, boolean enabled) {
        this.auditServiceUrl = auditServiceUrl;
        this.enabled = enabled;
        this.webClient = webClientBuilder.baseUrl(auditServiceUrl).build();

        log.info("AuditLogPublisher initialized: url={}, enabled={}", auditServiceUrl, enabled);
    }

    /**
     * Publish an audit log event to the audit service.
     * This method is async to not block the calling thread.
     */
    @Async
    public void publish(AuditLogEvent event) {
        if (!enabled) {
            log.trace("Audit logging disabled, skipping event: {}", event.getAuditId());
            return;
        }

        try {
            webClient.post()
                    .uri("/api/logs/audit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.trace("Published audit log: {}", event.getAuditId());
        } catch (Exception e) {
            log.error("Failed to publish audit log to {}: {}",
                    auditServiceUrl, e.getMessage());
            // Don't rethrow - audit logging should never fail business operations
        }
    }
}
