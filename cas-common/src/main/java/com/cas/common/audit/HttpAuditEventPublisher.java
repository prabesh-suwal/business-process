package com.cas.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * HTTP-based implementation of AuditEventPublisher.
 * Sends audit events synchronously to the audit service via REST API.
 * 
 * This bean is registered by {@link AuditServletAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
@Slf4j
@RequiredArgsConstructor
public class HttpAuditEventPublisher implements AuditEventPublisher {

    private final WebClient.Builder webClientBuilder;
    private final AuditProperties properties;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(properties.getServiceUrl())
                    .build();
        }
        return webClient;
    }

    @Override
    public void publish(AuditEvent event) {
        if (!properties.isEnabled()) {
            log.debug("Audit logging is disabled, skipping event: {}", event.getEventId());
            return;
        }

        // Enrich event with context
        enrichEvent(event);

        try {
            getWebClient()
                    .post()
                    .uri("/api/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMillis(properties.getTimeoutMs()));

            log.debug("Published audit event: {} - {} on {}",
                    event.getAction(), event.getResourceType(), event.getResourceId());

        } catch (Exception e) {
            if (properties.isFailSilently()) {
                log.warn("Failed to publish audit event (failing silently): {}", e.getMessage());
            } else {
                throw new RuntimeException("Failed to publish audit event", e);
            }
        }
    }

    private void enrichEvent(AuditEvent event) {
        // Set service name if not provided (from config)
        if (event.getServiceName() == null) {
            event.setServiceName(properties.getServiceName());
        }

        // Set product code dynamically from request context (X-Product-Code header)
        // Only fall back to config default if not available in context
        if (event.getProductCode() == null) {
            String dynamicProductCode = AuditContext.getProductCode();
            if (dynamicProductCode != null) {
                event.setProductCode(dynamicProductCode);
            } else if (properties.getDefaultProductCode() != null) {
                event.setProductCode(properties.getDefaultProductCode());
            }
        }

        // Set correlation ID from MDC if not provided
        if (event.getCorrelationId() == null) {
            event.setCorrelationId(AuditContext.getCorrelationId());
        }
    }
}
