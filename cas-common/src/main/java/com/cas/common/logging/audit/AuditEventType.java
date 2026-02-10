package com.cas.common.logging.audit;

/**
 * Audit event types for compliance logging.
 * Used to categorize what type of audit-worthy action occurred.
 */
public enum AuditEventType {
    // CRUD Operations
    CREATE,
    READ,
    UPDATE,
    DELETE,

    // Workflow/Approval Actions
    APPROVE,
    REJECT,
    SUBMIT,
    CANCEL,
    RETURN,
    ESCALATE,

    // Authentication
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGE,
    PASSWORD_RESET,

    // Authorization
    GRANT_ROLE,
    REVOKE_ROLE,
    GRANT_PERMISSION,
    REVOKE_PERMISSION,

    // Status Changes
    ACTIVATE,
    DEACTIVATE,
    ARCHIVE,
    RESTORE,

    // Data Operations
    EXPORT,
    IMPORT,
    BULK_UPDATE,
    BULK_DELETE,

    // Task/Workflow Actions
    CLAIM,
    UNCLAIM,
    DELEGATE,
    COMPLETE,
    STATUS_CHANGE,

    // Policy Actions
    CREATE_POLICY,
    UPDATE_POLICY,
    DELETE_POLICY
}
