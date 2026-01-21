package com.enterprise.lms.dto;

import com.enterprise.lms.entity.LoanApplication.ApplicationType;
import com.enterprise.lms.entity.LoanApplication.CoApplicant;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLoanApplicationRequest {

    // Loan product
    private String loanProductCode;

    // Application type
    @Builder.Default
    private ApplicationType applicationType = ApplicationType.NEW;
    private UUID parentLoanId; // For top-up/renewal
    private UUID parentApplicationId;

    // Primary applicant
    private UUID customerId;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    // Co-applicants
    private List<CoApplicant> coApplicants;

    // Loan details
    private BigDecimal requestedAmount;
    private Integer requestedTenure;
    private BigDecimal topupAmount; // For top-up
    private String loanPurpose;

    // Organization
    private UUID branchId;

    // Form data
    private Map<String, Object> applicationData;
}
