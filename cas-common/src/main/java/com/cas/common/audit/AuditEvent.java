package com.cas.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event representing a business action for compliance tracking.
 * This is the core DTO used to publish audit events to the audit service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * Unique identifier for this event.
     */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * Correlation ID for cross-service request tracing.
     */
    private String correlationId;

    /**
     * When the event occurred.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Name of the service that generated this event.
     */
    private String serviceName;

    /**
     * Product code for product-wise filtering (LMS, WFM, MMS, CAS, etc.)
     */
    private String productCode;

    // ===== WHO (Actor Information) =====

    /**
     * User ID or system identifier (UUID for internal tracking).
     */
    private String actorId;

    /**
     * Display name of the actor for UI (e.g., "John Doe").
     */
    private String actorName;

    /**
     * Email of the actor for UI display.
     */
    private String actorEmail;

    /**
     * Type of actor (USER, SYSTEM, API_CLIENT).
     */
    private ActorType actorType;

    /**
     * Roles of the actor at the time of action.
     */
    private List<String> actorRoles;

    /**
     * IP address of the actor.
     */
    private String ipAddress;

    // ===== WHAT (Action Information) =====

    /**
     * The action performed.
     */
    private AuditAction action;

    /**
     * Category of the action.
     */
    private AuditCategory category;

    /**
     * Type of resource being acted upon (e.g., MEMO, USER, POLICY).
     */
    private String resourceType;

    /**
     * ID of the specific resource being acted upon.
     */
    private String resourceId;

    // ===== CONTEXT =====

    /**
     * Human-readable description of the action.
     */
    private String description;

    /**
     * Additional metadata (before/after values, request params, etc.)
     */
    private Map<String, Object> metadata;

    /**
     * Result of the action.
     */
    @Builder.Default
    private AuditResult result = AuditResult.SUCCESS;

    /**
     * Reason for failure if result is FAILURE.
     */
    private String failureReason;
}
