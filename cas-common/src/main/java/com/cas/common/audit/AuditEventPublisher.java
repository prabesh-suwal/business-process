package com.cas.common.audit;

/**
 * Interface for publishing audit events.
 * This abstraction allows switching between sync and async implementations.
 */
public interface AuditEventPublisher {

    /**
     * Publish an audit event synchronously.
     * In the current implementation, this sends the event via HTTP.
     * 
     * @param event the audit event to publish
     */
    void publish(AuditEvent event);

    /**
     * Publish an audit event asynchronously.
     * Future implementation will use Kafka/RabbitMQ.
     * For now, falls back to sync publishing.
     * 
     * @param event the audit event to publish
     */
    default void publishAsync(AuditEvent event) {
        publish(event);
    }
}
