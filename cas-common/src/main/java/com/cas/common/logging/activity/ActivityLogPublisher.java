package com.cas.common.logging.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;

/**
 * HTTP publisher for activity log events.
 * Sends activity logs to the centralized audit-service.
 * 
 * This bean is registered by
 * {@link com.cas.common.logging.LoggingAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class ActivityLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogPublisher.class);

    private final RestClient restClient;
    private final String auditServiceUrl;
    private final boolean enabled;

    public ActivityLogPublisher(String auditServiceUrl, boolean enabled) {
        this.auditServiceUrl = auditServiceUrl;
        this.enabled = enabled;
        this.restClient = RestClient.builder()
                .baseUrl(auditServiceUrl)
                .build();

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
            restClient.post()
                    .uri("/api/logs/activity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();

            log.trace("Published activity log: {}", event.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish activity log to {}: {}",
                    auditServiceUrl, e.getMessage());
            // Don't rethrow - activity logging should never fail business operations
        }
    }
}
