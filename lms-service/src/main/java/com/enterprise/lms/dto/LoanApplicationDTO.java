package com.enterprise.lms.dto;

import com.enterprise.lms.entity.LoanApplication.ApplicationStatus;
import com.enterprise.lms.entity.LoanApplication.ApplicationType;
import com.enterprise.lms.entity.LoanApplication.CoApplicant;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationDTO {

    private UUID id;
    private String applicationNumber;
    private UUID loanProductId;
    private String loanProductCode;
    private String loanProductName;

    // Application type
    private ApplicationType applicationType;
    private UUID parentLoanId;
    private UUID parentApplicationId;

    // Primary applicant (link to Person Master)
    private UUID personId;
    private UUID customerId;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    // Co-applicants
    private List<CoApplicant> coApplicants;

    // Loan details
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer requestedTenure;
    private Integer approvedTenure;
    private BigDecimal topupAmount;
    private String loanPurpose;

    // Status
    private ApplicationStatus status;
    private String subStatus;

    // Workflow
    private String processInstanceId;
    private String currentTaskId;
    private String currentTaskName;
    private String currentTaskAssignee;
    private LocalDateTime taskAssignedAt;
    private LocalDateTime taskSlaDeadline;

    // Form data
    private Map<String, Object> applicationData;
    private Integer formVersionUsed;

    // Organization
    private UUID branchId;
    private String branchName;

    // Decision
    private UUID decidedBy;
    private String decidedByName;
    private LocalDateTime decidedAt;
    private String decisionComments;
    private String rejectionReason;

    // Audit
    private UUID createdBy;
    private String createdByName;
    private UUID submittedBy;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
