package com.cas.common.audit;

/**
 * Audit action types for all business events.
 */
public enum AuditAction {
    // Authentication
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    TOKEN_ISSUED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,

    // Data Operations
    CREATE,
    READ,
    UPDATE,
    DELETE,
    EXPORT,
    IMPORT,

    // Workflow Operations
    SUBMIT,
    APPROVE,
    REJECT,
    SEND_BACK,
    CANCEL,
    ASSIGN,
    REASSIGN,
    COMPLETE,
    CLAIM,
    UNCLAIM,
    DELEGATE,
    STATUS_CHANGE,

    // Admin Operations
    GRANT_ROLE,
    REVOKE_ROLE,
    CREATE_POLICY,
    UPDATE_POLICY,
    DELETE_POLICY,
    ACTIVATE,
    DEACTIVATE,

    // System Operations
    SERVICE_STARTED,
    SERVICE_STOPPED,
    CONFIG_CHANGED,

    // Access
    ACCESS_GRANTED,
    ACCESS_DENIED
}
