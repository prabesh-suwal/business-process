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
 * Loan Application - represents an individual loan application.
 * Supports top-up, renewal, joint applicants, and workflow integration.
 */
@Entity
@Table(name = "loan_application", indexes = {
        @Index(name = "idx_app_number", columnList = "application_number"),
        @Index(name = "idx_app_status", columnList = "status"),
        @Index(name = "idx_app_customer", columnList = "customer_id"),
        @Index(name = "idx_app_branch", columnList = "branch_id"),
        @Index(name = "idx_app_process", columnList = "process_instance_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_number", nullable = false, unique = true)
    private String applicationNumber; // "LN-2026-00001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    // === APPLICATION TYPE ===

    @Column(name = "application_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationType applicationType = ApplicationType.NEW;

    // For TOP-UP or RENEWAL - link to parent loan
    @Column(name = "parent_loan_id")
    private UUID parentLoanId;

    @Column(name = "parent_application_id")
    private UUID parentApplicationId;

    // === PRIMARY APPLICANT ===

    // Link to Person Master (person-service)
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "applicant_email")
    private String applicantEmail;

    @Column(name = "applicant_phone")
    private String applicantPhone;

    // === CO-APPLICANTS ===

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "co_applicants", columnDefinition = "jsonb")
    private List<CoApplicant> coApplicants;

    // === LOAN DETAILS ===

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "requested_tenure")
    private Integer requestedTenure; // months

    @Column(name = "approved_tenure")
    private Integer approvedTenure;

    // For top-up: additional amount
    @Column(name = "topup_amount")
    private BigDecimal topupAmount;

    // Purpose of loan
    @Column(name = "loan_purpose")
    private String loanPurpose;

    // === STATUS ===

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "sub_status")
    private String subStatus; // Custom sub-status for detailed tracking

    // === WORKFLOW INTEGRATION ===

    // Flowable process instance ID for workflow tracking
    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "current_task_id")
    private String currentTaskId;

    @Column(name = "current_task_name")
    private String currentTaskName;

    @Column(name = "current_task_assignee")
    private String currentTaskAssignee;

    // When current task was assigned
    @Column(name = "task_assigned_at")
    private LocalDateTime taskAssignedAt;

    // SLA deadline for current task
    @Column(name = "task_sla_deadline")
    private LocalDateTime taskSlaDeadline;

    // === FORM DATA ===

    // All application form data stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "application_data", columnDefinition = "jsonb")
    private Map<String, Object> applicationData;

    // Form version used (for immutability tracking)
    @Column(name = "form_version_used")
    private Integer formVersionUsed;

    // === DECISION ===

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_by_name")
    private String decidedByName;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_comments", columnDefinition = "TEXT")
    private String decisionComments;

    // Rejection reason code
    @Column(name = "rejection_reason")
    private String rejectionReason;

    // === ORGANIZATION ===

    // Branch where application was submitted
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "branch_name")
    private String branchName;

    // === AUDIT ===

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === ENUMS ===

    public enum ApplicationType {
        NEW, // Fresh application
        TOPUP, // Additional amount on existing loan
        RENEWAL, // Renew/extend existing loan
        AMENDMENT // Modify approved but not disbursed loan
    }

    public enum ApplicationStatus {
        DRAFT, // Not yet submitted
        SUBMITTED, // Submitted, workflow started
        UNDER_REVIEW, // Being processed
        PENDING_DOCS, // Waiting for documents
        PENDING_APPROVAL, // Waiting for approval decision
        APPROVED, // Approved
        CONDITIONALLY_APPROVED, // Approved with conditions
        REJECTED, // Rejected
        CANCELLED, // Cancelled by applicant
        ON_HOLD, // Temporarily on hold
        DISBURSEMENT_PENDING, // Approved, pending disbursement
        DISBURSED // Loan disbursed
    }

    // === EMBEDDED CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoApplicant {
        private UUID customerId;
        private String name;
        private String email;
        private String phone;
        private CoApplicantRole role;
        private Map<String, Object> details; // Additional info

        public enum CoApplicantRole {
            CO_BORROWER, // Equally liable
            GUARANTOR, // Guarantees repayment
            CO_SIGNER // Signs but not primary
        }
    }

    // === HELPER METHODS ===

    /**
     * Check if application can be edited.
     */
    public boolean isEditable() {
        return status == ApplicationStatus.DRAFT;
    }

    /**
     * Check if application is in a terminal state.
     */
    public boolean isTerminal() {
        return status == ApplicationStatus.REJECTED ||
                status == ApplicationStatus.CANCELLED ||
                status == ApplicationStatus.DISBURSED;
    }
}
