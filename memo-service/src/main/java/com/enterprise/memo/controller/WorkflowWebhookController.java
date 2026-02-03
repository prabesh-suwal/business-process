package com.enterprise.memo.controller;

import com.enterprise.memo.entity.MemoTask;
import com.enterprise.memo.service.MemoTaskService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Webhook endpoint for workflow-service to notify memo-service of events.
 * 
 * IMPORTANT: Assignment resolution is now done in workflow-service
 * (centralized).
 * This webhook only handles memo-specific business logic (creating MemoTask
 * records).
 */
@RestController
@RequestMapping("/api/workflow-webhook")
@RequiredArgsConstructor
@Slf4j
public class WorkflowWebhookController {

    private final MemoTaskService taskService;

    /**
     * Called by workflow-service when a task is created.
     * 
     * Assignment is already resolved by workflow-service.
     * We just create the MemoTask business record with the passed assignment info.
     */
    @PostMapping("/task-created")
    public ResponseEntity<Void> onTaskCreated(@RequestBody TaskCreatedEvent event) {
        log.info("Webhook: Task created - {} for memo {} with groups: {}",
                event.getTaskId(), event.getMemoId(), event.getCandidateGroups());

        // Create MemoTask record with assignment info from workflow-service
        MemoTask task = taskService.onTaskCreated(
                event.getTaskId(),
                event.getMemoId(),
                event.getTaskDefinitionKey(),
                event.getTaskName(),
                event.getCandidateGroups(),
                event.getCandidateUsers());

        log.info("Created MemoTask {} for workflow task {}", task.getId(), event.getTaskId());

        return ResponseEntity.ok().build();
    }

    /**
     * Called by workflow-service when a task is completed.
     */
    @PostMapping("/task-completed")
    public ResponseEntity<Void> onTaskCompleted(@RequestBody TaskCompletedEvent event) {
        log.info("Webhook: Task completed - {} by {}, action: {}",
                event.getTaskId(), event.getCompletedBy(), event.getAction());

        // Update MemoTask status to COMPLETED
        taskService.getByWorkflowTaskId(event.getTaskId()).ifPresent(task -> {
            task.setStatus(MemoTask.TaskStatus.COMPLETED);
            task.setCompletedAt(java.time.LocalDateTime.now());
            if (event.getAction() != null) {
                try {
                    task.setActionTaken(MemoTask.TaskAction.valueOf(event.getAction().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown action: {}", event.getAction());
                }
            }
            // Save is handled by transaction - need explicit save
            taskService.saveTask(task);
            log.info("Updated MemoTask {} to COMPLETED", task.getId());
        });

        return ResponseEntity.ok().build();
    }

    /**
     * Called by workflow-service when a process is completed.
     */
    @PostMapping("/process-completed")
    public ResponseEntity<Void> onProcessCompleted(@RequestBody ProcessCompletedEvent event) {
        log.info("Webhook: Process completed - {} status: {}", event.getProcessInstanceId(), event.getOutcome());
        return ResponseEntity.ok().build();
    }

    // Event DTOs
    @Data
    public static class TaskCreatedEvent {
        private String taskId;
        private UUID memoId;
        private String taskDefinitionKey;
        private String taskName;
        private String processInstanceId;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
    }

    @Data
    public static class TaskCompletedEvent {
        private String taskId;
        private String action;
        private String completedBy;
    }

    @Data
    public static class ProcessCompletedEvent {
        private String processInstanceId;
        private String outcome;
    }
}
