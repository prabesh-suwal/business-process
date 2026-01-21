package com.enterprise.lms.controller;

import com.enterprise.lms.entity.LoanApplication;
import com.enterprise.lms.entity.LoanApplication.ApplicationStatus;
import com.enterprise.lms.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Controller to receive workflow callbacks from workflow-service.
 * Updates loan application status based on workflow events.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow-callback")
@RequiredArgsConstructor
public class WorkflowCallbackController {

    private final LoanApplicationRepository loanApplicationRepository;

    /**
     * Receive workflow status callback.
     * Called by workflow-service when task/process status changes.
     */
    @PostMapping
    public ResponseEntity<Void> handleCallback(@RequestBody WorkflowEvent event) {
        log.info("Received workflow callback: {} for business key {}",
                event.getEventType(), event.getBusinessKey());

        try {
            switch (event.getEventType()) {
                case "TASK_CREATED":
                    handleTaskCreated(event);
                    break;
                case "TASK_COMPLETED":
                    handleTaskCompleted(event);
                    break;
                case "TASK_CLAIMED":
                    handleTaskClaimed(event);
                    break;
                case "PROCESS_COMPLETED":
                    handleProcessCompleted(event);
                    break;
                case "SLA_BREACHED":
                    handleSlaBreached(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling workflow callback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void handleTaskCreated(WorkflowEvent event) {
        UUID applicationId = parseBusinessKey(event.getBusinessKey());
        if (applicationId == null)
            return;

        loanApplicationRepository.findById(applicationId).ifPresent(app -> {
            app.setCurrentTaskId(event.getTaskId());
            app.setCurrentTaskName(event.getTaskName());
            app.setCurrentTaskAssignee(event.getAssignee());
            app.setTaskAssignedAt(LocalDateTime.now());

            // Update status based on task
            updateStatusFromTask(app, event.getTaskKey());

            loanApplicationRepository.save(app);
            log.info("Updated application {} with new task: {}", applicationId, event.getTaskName());
        });
    }

    private void handleTaskCompleted(WorkflowEvent event) {
        UUID applicationId = parseBusinessKey(event.getBusinessKey());
        if (applicationId == null)
            return;

        loanApplicationRepository.findById(applicationId).ifPresent(app -> {
            // Handle outcome-based updates
            String outcome = event.getOutcome();
            if ("APPROVED".equalsIgnoreCase(outcome)) {
                app.setStatus(ApplicationStatus.APPROVED);
                app.setDecidedAt(LocalDateTime.now());
            } else if ("REJECTED".equalsIgnoreCase(outcome)) {
                app.setStatus(ApplicationStatus.REJECTED);
                app.setDecidedAt(LocalDateTime.now());
            }

            // Clear current task (new task will set it)
            app.setCurrentTaskId(null);
            app.setCurrentTaskName(null);

            loanApplicationRepository.save(app);
            log.info("Task completed for application {}: outcome={}", applicationId, outcome);
        });
    }

    private void handleTaskClaimed(WorkflowEvent event) {
        UUID applicationId = parseBusinessKey(event.getBusinessKey());
        if (applicationId == null)
            return;

        loanApplicationRepository.findById(applicationId).ifPresent(app -> {
            app.setCurrentTaskAssignee(event.getAssignee());
            app.setTaskAssignedAt(LocalDateTime.now());
            loanApplicationRepository.save(app);
            log.info("Task claimed for application {} by {}", applicationId, event.getAssignee());
        });
    }

    private void handleProcessCompleted(WorkflowEvent event) {
        UUID applicationId = parseBusinessKey(event.getBusinessKey());
        if (applicationId == null)
            return;

        loanApplicationRepository.findById(applicationId).ifPresent(app -> {
            // If process ended without explicit approval/rejection
            if (app.getStatus() == ApplicationStatus.UNDER_REVIEW ||
                    app.getStatus() == ApplicationStatus.PENDING_APPROVAL) {
                app.setStatus(ApplicationStatus.APPROVED);
            }
            app.setProcessInstanceId(null);
            app.setCurrentTaskId(null);
            app.setCurrentTaskName(null);
            app.setSubStatus("WORKFLOW_COMPLETE");

            loanApplicationRepository.save(app);
            log.info("Process completed for application {}", applicationId);
        });
    }

    private void handleSlaBreached(WorkflowEvent event) {
        UUID applicationId = parseBusinessKey(event.getBusinessKey());
        if (applicationId == null)
            return;

        loanApplicationRepository.findById(applicationId).ifPresent(app -> {
            app.setSubStatus("SLA_BREACHED:" + event.getTaskKey());
            loanApplicationRepository.save(app);
            log.warn("SLA breached for application {} on task {}", applicationId, event.getTaskKey());
        });
    }

    private void updateStatusFromTask(LoanApplication app, String taskKey) {
        if (taskKey == null)
            return;

        // Map task keys to application statuses
        if (taskKey.contains("verify") || taskKey.contains("review")) {
            app.setStatus(ApplicationStatus.UNDER_REVIEW);
        } else if (taskKey.contains("approval") || taskKey.contains("sanction")) {
            app.setStatus(ApplicationStatus.PENDING_APPROVAL);
        } else if (taskKey.contains("document")) {
            app.setStatus(ApplicationStatus.PENDING_DOCS);
        }
    }

    private UUID parseBusinessKey(String businessKey) {
        if (businessKey == null || businessKey.isEmpty())
            return null;
        try {
            // Business key format: "LOAN_APPLICATION:{uuid}"
            if (businessKey.startsWith("LOAN_APPLICATION:")) {
                return UUID.fromString(businessKey.substring(17));
            }
            // Try parsing as UUID directly
            return UUID.fromString(businessKey);
        } catch (Exception e) {
            log.warn("Could not parse business key: {}", businessKey);
            return null;
        }
    }

    // === Event DTO ===

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkflowEvent {
        private String eventType;
        private String timestamp;
        private String processInstanceId;
        private String businessKey;
        private String taskId;
        private String taskKey;
        private String taskName;
        private String assignee;
        private String outcome;
        private String processDefinitionKey;
        private String endReason;
    }
}
