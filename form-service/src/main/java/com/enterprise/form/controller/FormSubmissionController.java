package com.enterprise.form.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.form.dto.*;
import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.repository.FormDefinitionRepository;
import com.enterprise.form.service.FormSubmissionService;
import com.enterprise.form.service.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for form submissions.
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService submissionService;
    private final FormDefinitionRepository formDefinitionRepository;
    private final ValidationService validationService;

    /**
     * Submit form data.
     */
    @PostMapping
    public ResponseEntity<FormSubmissionDTO> submitForm(@Valid @RequestBody SubmitFormRequest request) {
        UserContext user = UserContextHolder.getContext();
        UUID submittedBy = user != null && user.getUserId() != null ? UUID.fromString(user.getUserId()) : null;
        String userName = user != null ? user.getName() : null;
        FormSubmissionDTO submission = submissionService.submitForm(request, submittedBy, userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(submission);
    }

    /**
     * Get submission by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormSubmissionDTO> getSubmission(@PathVariable UUID id) {
        return ResponseEntity.ok(submissionService.getSubmission(id));
    }

    /**
     * Get submissions for a process instance.
     */
    @GetMapping("/by-process/{processInstanceId}")
    public ResponseEntity<List<FormSubmissionDTO>> getSubmissionsByProcess(
            @PathVariable String processInstanceId) {

        return ResponseEntity.ok(submissionService.getSubmissionsByProcess(processInstanceId));
    }

    /**
     * Get submission for a task.
     */
    @GetMapping("/by-task/{taskId}")
    public ResponseEntity<FormSubmissionDTO> getSubmissionByTask(@PathVariable String taskId) {
        return ResponseEntity.ok(submissionService.getSubmissionByTask(taskId));
    }

    /**
     * Validate form data without submitting.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResultDTO> validateForm(
            @RequestParam UUID formId,
            @RequestBody Map<String, Object> data) {

        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        ValidationResultDTO result = validationService.validate(form, data);
        return ResponseEntity.ok(result);
    }
}
