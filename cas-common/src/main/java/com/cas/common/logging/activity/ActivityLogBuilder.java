package com.cas.common.logging.activity;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Fluent builder for creating and publishing activity log events.
 * Used for tracking user actions for timeline/support purposes.
 * 
 * Usage:
 * 
 * <pre>
 * activityLogger.log()
 *         .type(ActivityType.DOWNLOAD)
 *         .module("REPORTS")
 *         .entity("Report", reportId)
 *         .description("Downloaded sales report")
 *         .success();
 * </pre>
 */
public class ActivityLogBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogBuilder.class);

    private final ActivityLogPublisher publisher;
    private final ActivityLogEvent.ActivityLogEventBuilder builder;

    public ActivityLogBuilder(ActivityLogPublisher publisher) {
        this.publisher = publisher;
        this.builder = ActivityLogEvent.builder();

        // Auto-populate from context
        populateFromContext();
    }

    private void populateFromContext() {
        builder.correlationId(MDC.get("correlationId"));

        // Populate from UserContext
        UserContext userContext = UserContextHolder.getContext();
        if (userContext != null) {
            builder.userId(userContext.getUserId());
            builder.username(userContext.getName());
            // Use first role as primary role
            String primaryRole = userContext.getRoles() != null && !userContext.getRoles().isEmpty()
                    ? userContext.getRoles().iterator().next()
                    : null;
            builder.userRole(primaryRole);
        }

        // Populate from HttpServletRequest
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                builder.ipAddress(getClientIp(request));
                builder.deviceInfo(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.trace("Could not get request context for activity log: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ===== Required Fields =====

    public ActivityLogBuilder type(ActivityType type) {
        builder.activityType(type);
        return this;
    }

    public ActivityLogBuilder module(String moduleName) {
        builder.moduleName(moduleName);
        return this;
    }

    // ===== Optional Fields =====

    public ActivityLogBuilder entity(String entityName, Object entityId) {
        builder.entityName(entityName);
        builder.entityId(entityId != null ? entityId.toString() : null);
        return this;
    }

    public ActivityLogBuilder description(String description) {
        builder.description(description);
        return this;
    }

    public ActivityLogBuilder geoLocation(String geoLocation) {
        builder.geoLocation(geoLocation);
        return this;
    }

    // ===== User Override (if different from current user) =====

    public ActivityLogBuilder user(String userId, String username, String role) {
        builder.userId(userId);
        builder.username(username);
        builder.userRole(role);
        return this;
    }

    // ===== Terminal Operations =====

    /**
     * Publish the activity event as successful.
     */
    public void success() {
        builder.status("SUCCESS");
        publish();
    }

    /**
     * Publish the activity event as failed.
     */
    public void failed() {
        builder.status("FAILED");
        publish();
    }

    /**
     * Publish the activity event as failed with a description.
     */
    public void failed(String reason) {
        builder.status("FAILED");
        builder.description(reason);
        publish();
    }

    private void publish() {
        ActivityLogEvent event = builder.build();

        try {
            publisher.publish(event);
            log.debug("Published activity log: {} {} on {} {}",
                    event.getActivityType(),
                    event.getModuleName(),
                    event.getEntityName(),
                    event.getEntityId());
        } catch (Exception e) {
            // Never let activity logging fail the business operation
            log.error("Failed to publish activity log event: {}", e.getMessage(), e);
        }
    }
}
