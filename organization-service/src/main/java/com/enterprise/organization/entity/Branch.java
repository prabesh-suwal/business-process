package com.enterprise.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Branch - Physical office/branch locations.
 * 
 * Examples:
 * - Kathmandu Head Office
 * - Pokhara Branch
 * - Birgunj Sub-Branch
 */
@Entity
@Table(name = "branches")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // Branch code (e.g., KTM-001, PKR-002)

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "local_name", length = 150)
    private String localName;

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false, length = 30)
    @Builder.Default
    private BranchType branchType = BranchType.BRANCH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_location_id")
    @ToString.Exclude
    private GeoLocation geoLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_branch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Branch parentBranch;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum BranchType {
        HEAD_OFFICE, REGIONAL_OFFICE, BRANCH, SUB_BRANCH, EXTENSION_COUNTER, ATM
    }

    public enum Status {
        ACTIVE, INACTIVE, CLOSED, PENDING
    }
}
