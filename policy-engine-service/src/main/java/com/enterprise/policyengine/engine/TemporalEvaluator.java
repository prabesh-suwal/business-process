package com.enterprise.policyengine.engine;

import com.enterprise.policyengine.entity.PolicyRule;
import com.enterprise.policyengine.entity.TemporalCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;

/**
 * Evaluates temporal conditions for time-based access control.
 */
@Slf4j
@Component
public class TemporalEvaluator {

    // Default business hours
    private static final LocalTime DEFAULT_BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_BUSINESS_END = LocalTime.of(18, 0);

    /**
     * Evaluate if the temporal condition is satisfied at the current time.
     *
     * @param rule The policy rule with temporal conditions
     * @return true if temporal condition is satisfied, false otherwise
     */
    public boolean evaluate(PolicyRule rule) {
        if (!rule.hasTemporalCondition()) {
            return true; // No temporal restriction
        }

        ZoneId zoneId = getZoneId(rule.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        return switch (rule.getTemporalCondition()) {
            case NONE -> true;
            case BUSINESS_HOURS -> evaluateBusinessHours(rule, now);
            case WEEKDAYS_ONLY -> evaluateWeekdaysOnly(now);
            case WEEKENDS_ONLY -> evaluateWeekendsOnly(now);
            case WITHIN_PERIOD -> evaluateWithinPeriod(rule, now);
            case OUTSIDE_PERIOD -> !evaluateWithinPeriod(rule, now);
            case TIME_WINDOW -> evaluateTimeWindow(rule, now);
        };
    }

    /**
     * Evaluate business hours condition.
     */
    private boolean evaluateBusinessHours(PolicyRule rule, ZonedDateTime now) {
        LocalTime timeFrom = rule.getTimeFrom() != null ? rule.getTimeFrom() : DEFAULT_BUSINESS_START;
        LocalTime timeTo = rule.getTimeTo() != null ? rule.getTimeTo() : DEFAULT_BUSINESS_END;
        LocalTime currentTime = now.toLocalTime();

        // Also check it's a weekday
        DayOfWeek day = now.getDayOfWeek();
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        boolean withinHours = !currentTime.isBefore(timeFrom) && !currentTime.isAfter(timeTo);

        log.debug("Business hours check: time={}, from={}, to={}, weekday={}, result={}",
                currentTime, timeFrom, timeTo, isWeekday, withinHours && isWeekday);

        return withinHours && isWeekday;
    }

    /**
     * Evaluate weekdays only condition.
     */
    private boolean evaluateWeekdaysOnly(ZonedDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * Evaluate weekends only condition.
     */
    private boolean evaluateWeekendsOnly(ZonedDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Evaluate within period condition.
     */
    private boolean evaluateWithinPeriod(PolicyRule rule, ZonedDateTime now) {
        LocalDate today = now.toLocalDate();

        if (rule.getValidFrom() != null && today.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidUntil() != null && today.isAfter(rule.getValidUntil())) {
            return false;
        }
        return true;
    }

    /**
     * Evaluate time window condition (specific hours, any day).
     */
    private boolean evaluateTimeWindow(PolicyRule rule, ZonedDateTime now) {
        if (rule.getTimeFrom() == null || rule.getTimeTo() == null) {
            log.warn("TIME_WINDOW condition requires timeFrom and timeTo");
            return true; // Default to allowed if not configured
        }

        LocalTime currentTime = now.toLocalTime();
        return !currentTime.isBefore(rule.getTimeFrom()) &&
                !currentTime.isAfter(rule.getTimeTo());
    }

    /**
     * Get ZoneId from string, with fallback to system default.
     */
    private ZoneId getZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}', using system default", timezone);
            return ZoneId.systemDefault();
        }
    }
}
