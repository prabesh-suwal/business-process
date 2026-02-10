package com.cas.common.logging.audit;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for creating and publishing audit log events.
 * Provides an explicit, debuggable way to log audit events.
 * 
 * Usage:
 * 
 * <pre>
 * auditLogger.log()
 *         .eventType(AuditEventType.APPROVE)
 *         .module("LOAN")
 *         .entity("Loan", loan.getId())
 *         .businessKey(loan.getLoanNumber())
 *         .oldValue(oldLoan)
 *         .newValue(newLoan)
 *         .changedFields("status", "approvedAmount")
 *         .remarks("Approved after verification")
 *         .success();
 * </pre>
 */
public class AuditLogBuilder {

    private static final Logger log = LoggerFactory.getLogger(AuditLogBuilder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AuditLogPublisher publisher;
    private final String serviceName;
    private final AuditLogEvent.AuditLogEventBuilder builder;

    public AuditLogBuilder(AuditLogPublisher publisher, String serviceName) {
        this.publisher = publisher;
        this.serviceName = serviceName;
        this.builder = AuditLogEvent.builder();

        // Auto-populate from context
        populateFromContext();
    }

    private void populateFromContext() {
        builder.serviceName(serviceName);
        builder.correlationId(MDC.get("correlationId"));

        // Populate from UserContext
        UserContext userContext = UserContextHolder.getContext();
        if (userContext != null) {
            builder.performedByUserId(userContext.getUserId());
            builder.performedByUsername(userContext.getName());
            // Use first role as primary role
            String primaryRole = userContext.getRoles() != null && !userContext.getRoles().isEmpty()
                    ? userContext.getRoles().iterator().next()
                    : null;
            builder.performedByRole(primaryRole);
            builder.performedByDepartment(userContext.getDepartmentId());
            builder.performedByBranch(userContext.getBranchId());
        }

        // Populate from HttpServletRequest
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                builder.ipAddress(getClientIp(request));
                builder.deviceId(request.getHeader("X-Device-Id"));
                builder.sessionId(request.getSession(false) != null ? request.getSession().getId() : null);
            }
        } catch (Exception e) {
            // Request context not available (e.g., async processing)
            log.trace("Could not get request context for audit log: {}", e.getMessage());
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

    public AuditLogBuilder eventType(AuditEventType eventType) {
        builder.eventType(eventType);
        return this;
    }

    public AuditLogBuilder module(String moduleName) {
        builder.moduleName(moduleName);
        return this;
    }

    public AuditLogBuilder entity(String entityName, Object entityId) {
        builder.entityName(entityName);
        builder.entityId(entityId != null ? entityId.toString() : null);
        return this;
    }

    // ===== Optional Fields =====

    public AuditLogBuilder businessKey(String businessKey) {
        builder.businessKey(businessKey);
        return this;
    }

    public AuditLogBuilder parentEntity(String parentEntityId) {
        builder.parentEntityId(parentEntityId);
        return this;
    }

    public AuditLogBuilder action(String action) {
        builder.action(action);
        return this;
    }

    public AuditLogBuilder oldValue(Object oldValue) {
        builder.oldValue(oldValue);
        return this;
    }

    public AuditLogBuilder newValue(Object newValue) {
        builder.newValue(newValue);
        return this;
    }

    public AuditLogBuilder changedFields(String... fields) {
        builder.changedFields(Arrays.asList(fields));
        return this;
    }

    public AuditLogBuilder changedFields(List<String> fields) {
        builder.changedFields(fields);
        return this;
    }

    public AuditLogBuilder remarks(String remarks) {
        builder.remarks(remarks);
        return this;
    }

    public AuditLogBuilder approvalLevel(int level) {
        builder.approvalLevel(level);
        return this;
    }

    // ===== Actor Override (if different from current user) =====

    public AuditLogBuilder performedBy(String userId, String username, String role) {
        builder.performedByUserId(userId);
        builder.performedByUsername(username);
        builder.performedByRole(role);
        return this;
    }

    // ===== Terminal Operations =====

    /**
     * Publish the audit event as successful.
     */
    public void success() {
        builder.result("SUCCESS");
        publish();
    }

    /**
     * Publish the audit event with an error.
     */
    public void error(Exception e) {
        builder.result("FAILURE");
        builder.errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        publish();
    }

    /**
     * Publish the audit event with a custom error message.
     */
    public void error(String errorMessage) {
        builder.result("FAILURE");
        builder.errorMessage(errorMessage);
        publish();
    }

    private void publish() {
        AuditLogEvent event = builder.build();

        try {
            publisher.publish(event);
            log.debug("Published audit log: {} {} on {} {}",
                    event.getEventType(),
                    event.getAction(),
                    event.getEntityName(),
                    event.getEntityId());
        } catch (Exception e) {
            // Never let audit logging fail the business operation
            log.error("Failed to publish audit log event: {}", e.getMessage(), e);
        }
    }
}
