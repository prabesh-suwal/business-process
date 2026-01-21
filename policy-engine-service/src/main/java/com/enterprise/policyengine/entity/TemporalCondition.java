package com.enterprise.policyengine.entity;

/**
 * Temporal conditions for time-based access control.
 * 
 * Examples:
 * - BUSINESS_HOURS: Access only during 9am-6pm
 * - WEEKDAYS_ONLY: Access Monday-Friday
 * - WITHIN_PERIOD: Access between specific dates
 */
public enum TemporalCondition {
    /**
     * No time restriction.
     */
    NONE,

    /**
     * Only during business hours (configurable, default 9:00-18:00).
     */
    BUSINESS_HOURS,

    /**
     * Only on weekdays (Monday-Friday).
     */
    WEEKDAYS_ONLY,

    /**
     * Only on weekends (Saturday-Sunday).
     */
    WEEKENDS_ONLY,

    /**
     * Only within a specific date range.
     * Requires validFrom and validUntil fields.
     */
    WITHIN_PERIOD,

    /**
     * Only outside a specific date range.
     * Requires validFrom and validUntil fields.
     */
    OUTSIDE_PERIOD,

    /**
     * Only during specific time window.
     * Requires timeFrom and timeTo fields.
     */
    TIME_WINDOW
}
