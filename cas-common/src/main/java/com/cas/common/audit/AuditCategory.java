package com.cas.common.audit;

/**
 * Audit action categories for business events.
 */
public enum AuditCategory {
    AUTHENTICATION, // Login, logout, token events
    AUTHORIZATION, // Access control decisions
    ACCESS, // API access events (gateway)
    DATA_ACCESS, // CRUD on business entities
    DOCUMENT, // Document-related events
    ORGANIZATION, // Organization structure events
    WORKFLOW, // Workflow state changes
    SECURITY, // Security policy events
    ADMIN, // Administrative actions
    SYSTEM // System events
}
