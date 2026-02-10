package com.enterprise.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Activity Log entity for user timeline logging.
 * Tracks user actions for customer support and admin monitoring.
 * Retention: 1 year.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_activity_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_activity_logs_activity_type", columnList = "activity_type"),
        @Index(name = "idx_activity_logs_module_name", columnList = "module_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID activityId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    // User Info
    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Column(length = 255)
    private String username;

    @Column(name = "user_role", length = 100)
    private String userRole;

    // Activity Details
    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    // Context
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "geo_location", length = 100)
    private String geoLocation;
}
