package com.enterprise.person.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PersonRelationship - Links persons with family or business relationships.
 */
@Entity
@Table(name = "person_relationship", indexes = {
        @Index(name = "idx_rel_person", columnList = "person_id"),
        @Index(name = "idx_rel_related", columnList = "related_person_id"),
        @Index(name = "idx_rel_type", columnList = "relationship_type")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "person_id", "related_person_id", "relationship_type" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_person_id", nullable = false)
    private Person relatedPerson;

    @Column(name = "relationship_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RelationshipType {
        // Family
        SPOUSE,
        FATHER,
        MOTHER,
        SON,
        DAUGHTER,
        SIBLING,
        GRANDFATHER,
        GRANDMOTHER,
        GRANDSON,
        GRANDDAUGHTER,
        UNCLE,
        AUNT,
        NEPHEW,
        NIECE,
        COUSIN,
        IN_LAW,

        // Business
        BUSINESS_PARTNER,
        EMPLOYER,
        EMPLOYEE,

        // Other
        GUARDIAN,
        NOMINEE,
        OTHER
    }
}
