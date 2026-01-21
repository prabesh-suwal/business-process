package com.enterprise.lms.service;

import com.enterprise.lms.dto.CreateLoanApplicationRequest;
import com.enterprise.lms.dto.LoanApplicationDTO;
import com.enterprise.lms.entity.LoanApplication;
import com.enterprise.lms.entity.LoanApplication.ApplicationStatus;
import com.enterprise.lms.entity.LoanProduct;
import com.enterprise.lms.repository.LoanApplicationRepository;
import com.enterprise.lms.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanProductRepository loanProductRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.workflow-service.url}")
    private String workflowServiceUrl;

    public LoanApplicationDTO createApplication(CreateLoanApplicationRequest request, UUID submittedBy) {
        // Get loan product
        LoanProduct product = loanProductRepository.findByCode(request.getLoanProductCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getLoanProductCode()));

        // Validate amount against product limits
        if (request.getRequestedAmount().compareTo(product.getMinAmount()) < 0) {
            throw new IllegalArgumentException("Requested amount below minimum: " + product.getMinAmount());
        }
        if (request.getRequestedAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds maximum: " + product.getMaxAmount());
        }

        // Validate tenure
        if (product.getMinTenure() != null && request.getRequestedTenure() < product.getMinTenure()) {
            throw new IllegalArgumentException("Requested tenure below minimum: " + product.getMinTenure() + " months");
        }
        if (product.getMaxTenure() != null && request.getRequestedTenure() > product.getMaxTenure()) {
            throw new IllegalArgumentException(
                    "Requested tenure exceeds maximum: " + product.getMaxTenure() + " months");
        }

        // Generate application number
        String applicationNumber = generateApplicationNumber();

        LoanApplication application = LoanApplication.builder()
                .applicationNumber(applicationNumber)
                .loanProduct(product)
                .customerId(request.getCustomerId())
                .applicantName(request.getApplicantName())
                .applicantEmail(request.getApplicantEmail())
                .applicantPhone(request.getApplicantPhone())
                .requestedAmount(request.getRequestedAmount())
                .requestedTenure(request.getRequestedTenure())
                .interestRate(product.getInterestRate())
                .branchId(request.getBranchId())
                .applicationData(request.getApplicationData())
                .status(ApplicationStatus.DRAFT)
                .submittedBy(submittedBy)
                .build();

        application = loanApplicationRepository.save(application);
        log.info("Created loan application: {}", applicationNumber);

        return toDTO(application);
    }

    public LoanApplicationDTO submitApplication(UUID applicationId, UUID submittedBy) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application already submitted. Status: " + application.getStatus());
        }

        // Start workflow if product has workflow template
        LoanProduct product = application.getLoanProduct();
        if (product.getWorkflowTemplateId() != null) {
            String processInstanceId = startWorkflow(application, product.getWorkflowTemplateId());
            application.setProcessInstanceId(processInstanceId);
        }

        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        application.setSubmittedBy(submittedBy);

        application = loanApplicationRepository.save(application);
        log.info("Submitted loan application: {} (Process: {})",
                application.getApplicationNumber(), application.getProcessInstanceId());

        return toDTO(application);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationDTO> getAllApplications() {
        return loanApplicationRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationDTO getApplication(UUID id) {
        return loanApplicationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    @Transactional(readOnly = true)
    public LoanApplicationDTO getApplicationByNumber(String applicationNumber) {
        return loanApplicationRepository.findByApplicationNumber(applicationNumber)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationNumber));
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationDTO> getMyApplications(UUID userId) {
        return loanApplicationRepository.findBySubmittedByOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        return loanApplicationRepository.findByStatus(status)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public LoanApplicationDTO updateApplicationData(UUID id, Map<String, Object> data) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Cannot update submitted application");
        }

        // Merge data
        Map<String, Object> existingData = application.getApplicationData();
        if (existingData == null) {
            existingData = new HashMap<>();
        }
        existingData.putAll(data);
        application.setApplicationData(existingData);

        application = loanApplicationRepository.save(application);
        return toDTO(application);
    }

    public LoanApplicationDTO updateStatus(UUID id, ApplicationStatus status, String comments, UUID updatedBy) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        application.setStatus(status);

        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            application.setDecidedAt(LocalDateTime.now());
            application.setDecidedBy(updatedBy);
            application.setDecisionComments(comments);
        }

        application = loanApplicationRepository.save(application);
        log.info("Updated application {} status to {}", application.getApplicationNumber(), status);

        return toDTO(application);
    }

    private String generateApplicationNumber() {
        String prefix = "LN-" + LocalDate.now().getYear() + "-";
        Integer maxSeq = loanApplicationRepository.findMaxApplicationNumberSequence(prefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    private String startWorkflow(LoanApplication application, UUID workflowTemplateId) {
        try {
            // Call workflow service to start process
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicationId", application.getId().toString());
            variables.put("applicationNumber", application.getApplicationNumber());
            variables.put("applicantName", application.getApplicantName());
            variables.put("requestedAmount", application.getRequestedAmount());
            variables.put("loanProductCode", application.getLoanProduct().getCode());

            Map<String, Object> request = Map.of(
                    "processTemplateId", workflowTemplateId.toString(),
                    "businessKey", application.getApplicationNumber(),
                    "variables", variables);

            // Make async call to workflow service
            Map response = webClientBuilder.build()
                    .post()
                    .uri(workflowServiceUrl + "/api/process-instances")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("processInstanceId") != null) {
                return response.get("processInstanceId").toString();
            }
        } catch (Exception e) {
            log.error("Failed to start workflow for application {}: {}",
                    application.getApplicationNumber(), e.getMessage());
        }
        return null;
    }

    private LoanApplicationDTO toDTO(LoanApplication app) {
        return LoanApplicationDTO.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .loanProductId(app.getLoanProduct().getId())
                .loanProductCode(app.getLoanProduct().getCode())
                .loanProductName(app.getLoanProduct().getName())
                // Application type
                .applicationType(app.getApplicationType())
                .parentLoanId(app.getParentLoanId())
                .parentApplicationId(app.getParentApplicationId())
                // Primary applicant
                .personId(app.getPersonId())
                .customerId(app.getCustomerId())
                .applicantName(app.getApplicantName())
                .applicantEmail(app.getApplicantEmail())
                .applicantPhone(app.getApplicantPhone())
                // Co-applicants
                .coApplicants(app.getCoApplicants())
                // Loan details
                .requestedAmount(app.getRequestedAmount())
                .approvedAmount(app.getApprovedAmount())
                .interestRate(app.getInterestRate())
                .requestedTenure(app.getRequestedTenure())
                .approvedTenure(app.getApprovedTenure())
                .topupAmount(app.getTopupAmount())
                .loanPurpose(app.getLoanPurpose())
                // Status
                .status(app.getStatus())
                .subStatus(app.getSubStatus())
                // Workflow
                .processInstanceId(app.getProcessInstanceId())
                .currentTaskId(app.getCurrentTaskId())
                .currentTaskName(app.getCurrentTaskName())
                .currentTaskAssignee(app.getCurrentTaskAssignee())
                .taskAssignedAt(app.getTaskAssignedAt())
                .taskSlaDeadline(app.getTaskSlaDeadline())
                // Form data
                .applicationData(app.getApplicationData())
                .formVersionUsed(app.getFormVersionUsed())
                // Organization
                .branchId(app.getBranchId())
                .branchName(app.getBranchName())
                // Decision
                .decidedBy(app.getDecidedBy())
                .decidedByName(app.getDecidedByName())
                .decidedAt(app.getDecidedAt())
                .decisionComments(app.getDecisionComments())
                .rejectionReason(app.getRejectionReason())
                // Audit
                .createdBy(app.getCreatedBy())
                .createdByName(app.getCreatedByName())
                .submittedBy(app.getSubmittedBy())
                .submittedByName(app.getSubmittedByName())
                .submittedAt(app.getSubmittedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
