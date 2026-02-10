package com.cas.common.logging.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Activity Log event for user timeline logging.
 * Tracks user actions for customer support and admin monitoring.
 * 
 * Retention: 1 year
 * Audience: Support, admins, end-users (their own activities)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogEvent {

    @Builder.Default
    private String activityId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String correlationId;

    // User Information
    private String userId;
    private String username;
    private String userRole;

    // Activity Details
    private ActivityType activityType;
    private String moduleName;
    private String entityName;
    private String entityId;
    private String description;

    // Result
    @Builder.Default
    private String status = "SUCCESS"; // SUCCESS or FAILED

    // Context
    private String ipAddress;
    private String deviceInfo;
    private String geoLocation;
}
