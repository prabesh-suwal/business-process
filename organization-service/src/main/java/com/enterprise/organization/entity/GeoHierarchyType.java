package com.enterprise.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * GeoHierarchyType - Defines hierarchy levels for geographical locations.
 * 
 * This is configurable per country to support:
 * - Nepal: Country → Province → District → Municipality → Ward
 * - USA: Country → State → County → City → ZIP
 * - India: Country → State → District → Block → Village
 */
@Entity
@Table(name = "geo_hierarchy_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "country_code", "code" })
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoHierarchyType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode; // ISO 3166-1 alpha-2/3 (NP, US, IN)

    @Column(nullable = false, length = 50)
    private String code; // PROVINCE, DISTRICT, MUNICIPALITY, etc.

    @Column(nullable = false, length = 100)
    private String name; // Human-readable name

    @Column(name = "local_name", length = 100)
    private String localName; // Local language name (e.g., "प्रदेश" for Province)

    @Column(nullable = false)
    private Integer level; // Hierarchy level (1 = top level)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_type_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private GeoHierarchyType parentType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
