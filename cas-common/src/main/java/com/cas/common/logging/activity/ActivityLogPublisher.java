package com.cas.common.logging.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP publisher for activity log events.
 * Sends activity logs to the centralized audit-service.
 * 
 * Uses WebClient (with UserContextWebClientFilter) to automatically
 * propagate user context headers for service-to-service calls.
 * 
 * This bean is registered by
 * {@link com.cas.common.logging.LoggingAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class ActivityLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogPublisher.class);

    private final WebClient webClient;
    private final String auditServiceUrl;
    private final boolean enabled;

    public ActivityLogPublisher(WebClient.Builder webClientBuilder, String auditServiceUrl, boolean enabled) {
        this.auditServiceUrl = auditServiceUrl;
        this.enabled = enabled;
        this.webClient = webClientBuilder.baseUrl(auditServiceUrl).build();

        log.info("ActivityLogPublisher initialized: url={}, enabled={}", auditServiceUrl, enabled);
    }

    /**
     * Publish an activity log event to the audit service.
     * This method is async to not block the calling thread.
     */
    @Async
    public void publish(ActivityLogEvent event) {
        if (!enabled) {
            log.trace("Activity logging disabled, skipping event: {}", event.getActivityId());
            return;
        }

        try {
            webClient.post()
                    .uri("/api/logs/activity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.trace("Published activity log: {}", event.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish activity log to {}: {}",
                    auditServiceUrl, e.getMessage());
            // Don't rethrow - activity logging should never fail business operations
        }
    }
}
