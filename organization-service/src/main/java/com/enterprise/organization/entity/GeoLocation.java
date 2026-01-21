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
 * GeoLocation - Actual geographical units (provinces, districts,
 * municipalities).
 * 
 * Examples:
 * - Bagmati Province (type=PROVINCE, country=NP)
 * - Kathmandu District (type=DISTRICT, parent=Bagmati Province)
 * - Kathmandu Metropolitan City (type=MUNICIPALITY, parent=Kathmandu District)
 */
@Entity
@Table(name = "geo_locations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "country_code", "code" })
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(nullable = false, length = 50)
    private String code; // Unique code within country (e.g., NP-BA, NP-KTM)

    @Column(nullable = false, length = 150)
    private String name; // English name

    @Column(name = "local_name", length = 150)
    private String localName; // Local language name (e.g., "बागमती प्रदेश")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    @ToString.Exclude
    private GeoHierarchyType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private GeoLocation parent;

    @Column(name = "full_path", length = 500)
    private String fullPath; // Materialized path: "/NP/BAGMATI/KATHMANDU/KMC"

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata; // Additional data (population, area, etc.)

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        ACTIVE, INACTIVE, DEPRECATED
    }
}
