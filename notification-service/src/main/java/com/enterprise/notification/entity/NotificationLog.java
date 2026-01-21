package com.enterprise.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notification log - tracks all sent notifications for audit and retry.
 */
@Entity
@Table(name = "notification_log", indexes = {
        @Index(name = "idx_log_recipient", columnList = "recipient"),
        @Index(name = "idx_log_status", columnList = "status"),
        @Index(name = "idx_log_entity", columnList = "linked_entity_type, linked_entity_id"),
        @Index(name = "idx_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @Column(name = "template_code")
    private String templateCode; // Denormalized for quick filtering

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationTemplate.Channel channel;

    @Column(nullable = false)
    private String recipient; // Email address, phone number, user ID

    @Column(name = "recipient_name")
    private String recipientName;

    @Column
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    // Variables used for template rendering
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    // Link to business entity
    @Column(name = "linked_entity_type")
    private String linkedEntityType;

    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    // Trigger info
    @Column(name = "triggered_by")
    private UUID triggeredBy; // User who triggered (null if system)

    @Column(name = "trigger_event")
    @Enumerated(EnumType.STRING)
    private NotificationTemplate.TriggerEvent triggerEvent;

    // Delivery tracking
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // External reference (e.g., email message ID)
    @Column(name = "external_reference")
    private String externalReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        SENDING,
        SENT,
        FAILED,
        CANCELLED
    }

    // Helper to increment retry
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
