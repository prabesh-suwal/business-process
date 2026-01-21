package com.enterprise.policyengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * PolicyRule - a single condition that must be satisfied.
 * 
 * Examples:
 * - attribute: "subject.permissions", operator: CONTAINS, value: "loan:approve"
 * - attribute: "resource.branchId", operator: IN, value: "subject.branchIds"
 * (expression)
 * - attribute: "resource.amount", operator: LESS_THAN_OR_EQUAL, value:
 * "subject.approvalLimit"
 * 
 * With temporal conditions:
 * - temporalCondition: BUSINESS_HOURS, timeFrom: "09:00", timeTo: "18:00"
 * - temporalCondition: WITHIN_PERIOD, validFrom: "2024-01-01", validUntil:
 * "2024-12-31"
 */
@Entity
@Table(name = "policy_rules")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @Column(name = "rule_group", length = 50)
    @Builder.Default
    private String ruleGroup = "default";

    @Column(nullable = false, length = 100)
    private String attribute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ValueType valueType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ==================== TEMPORAL CONDITIONS ====================

    /**
     * Type of temporal restriction (NONE, BUSINESS_HOURS, WEEKDAYS_ONLY, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "temporal_condition", length = 30)
    @Builder.Default
    private TemporalCondition temporalCondition = TemporalCondition.NONE;

    /**
     * Start time for TIME_WINDOW or BUSINESS_HOURS (e.g., "09:00")
     */
    @Column(name = "time_from")
    private LocalTime timeFrom;

    /**
     * End time for TIME_WINDOW or BUSINESS_HOURS (e.g., "18:00")
     */
    @Column(name = "time_to")
    private LocalTime timeTo;

    /**
     * Start date for WITHIN_PERIOD (e.g., "2024-01-01")
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * End date for WITHIN_PERIOD (e.g., "2024-12-31")
     */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /**
     * Timezone for temporal evaluations (default: system timezone)
     */
    @Column(name = "timezone", length = 50)
    private String timezone;

    // ==============================================================

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Check if this rule has a temporal condition.
     */
    public boolean hasTemporalCondition() {
        return temporalCondition != null && temporalCondition != TemporalCondition.NONE;
    }
}
