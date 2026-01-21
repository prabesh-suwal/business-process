package com.enterprise.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loan Product - defines types of loans available (Home, Vehicle, Personal,
 * etc.)
 */
@Entity
@Table(name = "loan_product", indexes = {
        @Index(name = "idx_loan_product_code", columnList = "code"),
        @Index(name = "idx_loan_product_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // "HOME_LOAN", "VEHICLE_LOAN"

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Loan type classification
    @Column(name = "loan_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanType loanType = LoanType.UNSECURED;

    // === AMOUNT & TERMS ===

    @Column(name = "min_amount", nullable = false)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false)
    private BigDecimal maxAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // Annual interest rate

    @Column(name = "min_tenure")
    private Integer minTenure; // months

    @Column(name = "max_tenure")
    private Integer maxTenure; // months

    @Column(name = "processing_fee_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal processingFeePercent = BigDecimal.ZERO;

    // === WORKFLOW & FORMS ===

    // Links to workflow template for this loan type
    @Column(name = "workflow_template_id")
    private UUID workflowTemplateId;

    // Links to form definition for application
    @Column(name = "application_form_id")
    private UUID applicationFormId;

    // Form version (for immutability)
    @Column(name = "application_form_version")
    private Integer applicationFormVersion;

    // === DOCUMENT REQUIREMENTS ===

    // Required documents for this product
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_checklist", columnDefinition = "jsonb")
    private List<DocumentRequirement> documentChecklist;

    // === CONFIGURATION ===

    @Column(name = "product_id")
    private UUID productId; // CAS product ID (LMS)

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Allow top-up on existing loans
    @Column(name = "allow_topup")
    @Builder.Default
    private Boolean allowTopup = false;

    // Allow renewal
    @Column(name = "allow_renewal")
    @Builder.Default
    private Boolean allowRenewal = false;

    // Allow joint applicants
    @Column(name = "allow_joint_applicants")
    @Builder.Default
    private Boolean allowJointApplicants = true;

    // Maximum co-applicants allowed
    @Column(name = "max_co_applicants")
    @Builder.Default
    private Integer maxCoApplicants = 3;

    // How to handle post-approval modifications
    @Column(name = "amendment_mode")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AmendmentMode amendmentMode = AmendmentMode.FRESH;

    // Eligibility criteria
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_criteria", columnDefinition = "jsonb")
    private Map<String, Object> eligibilityCriteria;

    // Additional configuration as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    // === AUDIT ===

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === ENUMS ===

    public enum LoanType {
        SECURED, // Collateral required (Home, Vehicle)
        UNSECURED, // No collateral (Personal)
        GOLD, // Gold loan
        MICROFINANCE // Group/microfinance loans
    }

    public enum AmendmentMode {
        FRESH, // Cancel and start new application
        PARTIAL // Only re-execute affected steps
    }

    // === EMBEDDED CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentRequirement {
        private String documentType; // ID_PROOF, INCOME_PROOF, etc.
        private String name;
        private String description;
        private boolean required;
        private boolean forCoApplicant; // Required for co-applicants too
    }
}
