package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores gateway decision rules for a memo topic.
 * These rules determine which sequence flow to take at each gateway.
 */
@Entity
@Table(name = "gateway_decision_rule", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "memo_topic_id", "gateway_key", "version" })
}, indexes = {
        @Index(name = "idx_gateway_rule_topic", columnList = "memo_topic_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayDecisionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_topic_id", nullable = false)
    private MemoTopic memoTopic;

    @Column(name = "gateway_key", nullable = false, length = 100)
    private String gatewayKey; // e.g., "gw_amountCheck"

    @Column(name = "gateway_name")
    private String gatewayName; // Human readable: "Amount Check"

    // Rules array - evaluated top to bottom, first match wins
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> rules;
    // Structure: [{conditions: [{field, operator, value}], goTo: "flowName"}]

    @Column(name = "default_flow", nullable = false)
    private String defaultFlow; // Sequence flow name if no match

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "activated_by")
    private UUID activatedBy;
}
