package com.cas.common.logging.activity;

/**
 * Activity types for user timeline logging.
 * Lighter than audit - tracks user actions for support/monitoring.
 */
public enum ActivityType {
    // Authentication
    LOGIN,
    LOGOUT,
    SESSION_EXPIRED,

    // Navigation/Viewing
    VIEW,
    SEARCH,
    FILTER,
    NAVIGATE,

    // File Operations
    DOWNLOAD,
    UPLOAD,
    PREVIEW,
    PRINT,

    // Form Actions
    APPLY,
    SUBMIT,
    SAVE_DRAFT,
    CANCEL,

    // Workflow Actions
    CLAIM,
    UNCLAIM,
    COMPLETE,
    DELEGATE,

    // Generic
    CLICK,
    EXPORT,
    SHARE
}
