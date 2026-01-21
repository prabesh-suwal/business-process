package com.enterprise.form.service;

import com.enterprise.form.dto.*;
import com.enterprise.form.entity.FileUpload;
import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.entity.FormSubmission;
import com.enterprise.form.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for form submissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FormSubmissionService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormSubmissionRepository submissionRepository;
    private final FileUploadRepository fileUploadRepository;
    private final ValidationService validationService;

    /**
     * Submit form data.
     */
    public FormSubmissionDTO submitForm(SubmitFormRequest request, UUID submittedBy, String submittedByName) {
        FormDefinition form = formDefinitionRepository.findById(request.getFormDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + request.getFormDefinitionId()));

        // Validate the data
        ValidationResultDTO validationResult = validationService.validate(form, request.getData());

        FormSubmission submission = FormSubmission.builder()
                .formDefinition(form)
                .processInstanceId(request.getProcessInstanceId())
                .taskId(request.getTaskId())
                .data(request.getData())
                .submittedBy(submittedBy)
                .submittedByName(submittedByName)
                .validationStatus(validationResult.isValid()
                        ? FormSubmission.ValidationStatus.VALID
                        : FormSubmission.ValidationStatus.INVALID)
                .validationErrors(validationResult.isValid() ? null
                        : validationResult.getErrors().stream()
                                .map(e -> java.util.Map.of(
                                        "field", (Object) e.getFieldKey(),
                                        "message", e.getMessage(),
                                        "rule", e.getRule()))
                                .collect(Collectors.toList()))
                .build();

        submission = submissionRepository.save(submission);
        log.info("Form submitted: {} for task {} by {}", submission.getId(), request.getTaskId(), submittedByName);

        return toDTO(submission);
    }

    /**
     * Get submission by ID.
     */
    @Transactional(readOnly = true)
    public FormSubmissionDTO getSubmission(UUID submissionId) {
        FormSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        return toDTO(submission);
    }

    /**
     * Get submissions for a process instance.
     */
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> getSubmissionsByProcess(String processInstanceId) {
        return submissionRepository.findByProcessInstanceIdOrderBySubmittedAtAsc(processInstanceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get submission for a task.
     */
    @Transactional(readOnly = true)
    public FormSubmissionDTO getSubmissionByTask(String taskId) {
        FormSubmission submission = submissionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("No submission found for task: " + taskId));
        return toDTO(submission);
    }

    private FormSubmissionDTO toDTO(FormSubmission submission) {
        List<FileUploadDTO> fileDTOs = submission.getFiles().stream()
                .map(this::toFileDTO)
                .collect(Collectors.toList());

        return FormSubmissionDTO.builder()
                .id(submission.getId())
                .formDefinitionId(submission.getFormDefinition().getId())
                .formName(submission.getFormDefinition().getName())
                .processInstanceId(submission.getProcessInstanceId())
                .taskId(submission.getTaskId())
                .data(submission.getData())
                .submittedBy(submission.getSubmittedBy())
                .submittedByName(submission.getSubmittedByName())
                .submittedAt(submission.getSubmittedAt())
                .validationStatus(submission.getValidationStatus())
                .validationErrors(submission.getValidationErrors())
                .files(fileDTOs)
                .build();
    }

    private FileUploadDTO toFileDTO(FileUpload file) {
        return FileUploadDTO.builder()
                .id(file.getId())
                .fieldKey(file.getFieldKey())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .downloadUrl("/api/files/" + file.getId())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
