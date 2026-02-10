package com.cas.common.logging;

/**
 * Log type enumeration for the three-tier logging system.
 */
public enum LogType {
    API, // Technical/system logs for debugging
    AUDIT, // Compliance logs for legal/audit
    ACTIVITY // User activity logs for timeline
}
