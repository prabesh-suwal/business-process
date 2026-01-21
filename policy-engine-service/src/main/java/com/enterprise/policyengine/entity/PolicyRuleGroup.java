package com.enterprise.policyengine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * PolicyRuleGroup - defines how rules are combined (AND/OR).
 * 
 * Rules within the same group are combined using the group's logic operator.
 * By default, all groups must pass (AND between groups).
 */
@Entity
@Table(name = "policy_rule_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRuleGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @ToString.Exclude // IMPORTANT: Exclude the parent reference
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "logic_operator", length = 5)
    @Builder.Default
    private LogicOperator logicOperator = LogicOperator.AND;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    @ToString.Exclude
    private PolicyRuleGroup parentGroup;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
