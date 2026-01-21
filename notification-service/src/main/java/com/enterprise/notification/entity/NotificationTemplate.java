package com.enterprise.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notification template - defines reusable notification formats.
 * Templates use placeholders like ${userName}, ${applicationNumber}
 */
@Entity
@Table(name = "notification_template", indexes = {
        @Index(name = "idx_template_code", columnList = "code"),
        @Index(name = "idx_template_event", columnList = "trigger_event")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // e.g., "TASK_ASSIGNED", "APPLICATION_APPROVED"

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    // For EMAIL: subject line with placeholders
    @Column(name = "subject_template")
    private String subjectTemplate;

    // Body with placeholders: ${userName}, ${applicationNumber}, etc.
    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    // HTML body for rich emails (optional)
    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate;

    // Event that triggers this notification
    @Column(name = "trigger_event")
    @Enumerated(EnumType.STRING)
    private TriggerEvent triggerEvent;

    // Product-specific template (null = all products)
    @Column(name = "product_id")
    private UUID productId;

    // Default placeholders/variables this template expects
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_variables", columnDefinition = "jsonb")
    private Map<String, String> expectedVariables;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Channel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }

    public enum TriggerEvent {
        // Task events
        TASK_ASSIGNED,
        TASK_COMPLETED,
        TASK_DELEGATED,
        TASK_ESCALATED,

        // Application events
        APPLICATION_SUBMITTED,
        APPLICATION_APPROVED,
        APPLICATION_REJECTED,
        APPLICATION_RETURNED,

        // SLA events
        SLA_WARNING,
        SLA_BREACHED,

        // Document events
        DOCUMENT_UPLOADED,
        DOCUMENT_REQUIRED,

        // User events
        PASSWORD_RESET,
        ACCOUNT_CREATED,

        // Custom (manual trigger)
        CUSTOM
    }
}
