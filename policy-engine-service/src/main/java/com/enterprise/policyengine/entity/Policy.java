package com.enterprise.policyengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;

/**
 * Policy entity - represents an authorization policy.
 * 
 * A policy defines:
 * - What resource type it applies to (loan, customer, user, etc.)
 * - What action it governs (view, approve, delete, etc.)
 * - Whether to ALLOW or DENY when rules match
 * - The rules that must be satisfied
 */
@Entity
@Table(name = "policies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PolicyEffect effect = PolicyEffect.ALLOW;

    @Column
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column
    @Builder.Default
    private Integer version = 1;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent circular loop in toString
    @EqualsAndHashCode.Exclude // Prevent circular loop in equals/hashCode
    @Builder.Default
    private Set<PolicyRule> rules = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent circular loop in toString
    @EqualsAndHashCode.Exclude // Prevent circular loop in equals/hashCode
    @Builder.Default
    private Set<PolicyRuleGroup> ruleGroups = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_products", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "product_name")
    @Builder.Default
    private Set<String> products = new HashSet<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods
    public void addRule(PolicyRule rule) {
        rules.add(rule);
        rule.setPolicy(this);
    }

    public void removeRule(PolicyRule rule) {
        rules.remove(rule);
        rule.setPolicy(null);
    }

    public void addRuleGroup(PolicyRuleGroup group) {
        ruleGroups.add(group);
        group.setPolicy(this);
    }
}
