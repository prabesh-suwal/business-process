package com.enterprise.person.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * PersonRole - Tracks roles a person has across different products.
 * Example: Borrower in LMS, Signatory in Memo.
 */
@Entity
@Table(name = "person_role", indexes = {
        @Index(name = "idx_role_person", columnList = "person_id"),
        @Index(name = "idx_role_product", columnList = "product_id"),
        @Index(name = "idx_role_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_role_type", columnList = "role_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "role_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    // Product ID from CAS (LMS, Memo, etc.)
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_code")
    private String productCode; // "LMS", "MEMO"

    // Entity this role applies to
    @Column(name = "entity_type", nullable = false)
    private String entityType; // "LOAN_APPLICATION", "MEMO", "LOAN_GROUP"

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    // Additional role details
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "role_details", columnDefinition = "jsonb")
    private Map<String, Object> roleDetails; // share_percent, liability_type, etc.

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RoleType {
        // Loan roles
        BORROWER,
        CO_BORROWER,
        GUARANTOR,
        NOMINEE,

        // Group roles
        GROUP_LEADER,
        GROUP_MEMBER,

        // Document roles
        SIGNATORY,
        WITNESS,
        APPROVER,

        // Property roles
        OWNER,
        CO_OWNER,

        // Other
        REFERENCE,
        OTHER
    }
}
