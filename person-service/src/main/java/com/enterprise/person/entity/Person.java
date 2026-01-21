package com.enterprise.person.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Person - Central identity for customers, guarantors, signatories across all
 * products.
 */
@Entity
@Table(name = "person", indexes = {
        @Index(name = "idx_person_code", columnList = "person_code"),
        @Index(name = "idx_person_citizenship", columnList = "citizenship_number"),
        @Index(name = "idx_person_phone", columnList = "primary_phone"),
        @Index(name = "idx_person_email", columnList = "email"),
        @Index(name = "idx_person_name", columnList = "first_name, last_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "person_code", nullable = false, unique = true)
    private String personCode; // P-2026-00001

    // === IDENTITY ===

    @Column(name = "salutation", length = 10)
    @Enumerated(EnumType.STRING)
    private Salutation salutation;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "full_name")
    private String fullName; // Computed: firstName + middleName + lastName

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // === IDENTIFIERS ===

    @Column(name = "citizenship_number", unique = true)
    private String citizenshipNumber;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "photo_url")
    private String photoUrl;

    // === CONTACT ===

    @Column(name = "primary_phone")
    private String primaryPhone;

    @Column(name = "secondary_phone")
    private String secondaryPhone;

    @Column(name = "email")
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_address", columnDefinition = "jsonb")
    private Address currentAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permanent_address", columnDefinition = "jsonb")
    private Address permanentAddress;

    // === EMPLOYMENT/INCOME ===

    @Column(name = "occupation_type")
    @Enumerated(EnumType.STRING)
    private OccupationType occupationType;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;

    @Column(name = "annual_income")
    private BigDecimal annualIncome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "employment_details", columnDefinition = "jsonb")
    private Map<String, Object> employmentDetails;

    // === KYC ===

    @Column(name = "kyc_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private UUID kycVerifiedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kyc_documents", columnDefinition = "jsonb")
    private List<UUID> kycDocuments; // Document IDs from document-service

    // === METADATA ===

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "branch_id")
    private UUID branchId; // Branch where person was registered

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === RELATIONSHIPS ===

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    private List<PersonRelationship> relationships;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    private List<PersonRole> roles;

    // === ENUMS ===

    public enum Salutation {
        MR, MRS, MS, DR, PROF
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum OccupationType {
        SALARIED,
        SELF_EMPLOYED,
        BUSINESS,
        PROFESSIONAL,
        FARMER,
        RETIRED,
        STUDENT,
        HOMEMAKER,
        OTHER
    }

    public enum KycStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        REJECTED
    }

    // === EMBEDDED ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Address {
        private String street;
        private String city;
        private String district;
        private String province;
        private String country;
        private String postalCode;
    }

    // === LIFECYCLE ===

    @PrePersist
    @PreUpdate
    public void computeFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null)
            sb.append(firstName);
        if (middleName != null)
            sb.append(" ").append(middleName);
        if (lastName != null)
            sb.append(" ").append(lastName);
        this.fullName = sb.toString().trim();
    }
}
