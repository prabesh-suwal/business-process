package com.cas.server.domain.policy;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 50)
    private PolicyType policyType;

    @Column(nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rules;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column
    @Builder.Default
    private Integer priority = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum PolicyType {
        ACCESS, // Access control policies
        DATA, // Data filtering policies
        RATE_LIMIT, // Rate limiting policies
        TIME_BASED, // Time-based access policies
        IP_BASED // IP-based restrictions
    }

    public enum PolicyStatus {
        ACTIVE, INACTIVE, DRAFT
    }

    /**
     * Check if policy is global (applies to all products).
     */
    public boolean isGlobal() {
        return productCode == null;
    }
}
