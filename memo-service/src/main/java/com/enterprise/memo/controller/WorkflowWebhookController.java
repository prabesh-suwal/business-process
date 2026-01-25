package com.enterprise.memo.controller;

import com.enterprise.memo.entity.MemoTask;
import com.enterprise.memo.service.MemoTaskService;
import com.enterprise.memo.service.AssignmentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook endpoint for workflow-service to notify memo-service of events.
 * This is how workflow-service triggers memo-service without knowing business
 * logic.
 */
@RestController
@RequestMapping("/api/workflow-webhook")
@RequiredArgsConstructor
@Slf4j
public class WorkflowWebhookController {

    private final MemoTaskService taskService;
    private final AssignmentService assignmentService;

    /**
     * Called by workflow-service when a task is created.
     */
    @PostMapping("/task-created")
    public ResponseEntity<TaskCreatedResponse> onTaskCreated(@RequestBody TaskCreatedEvent event) {
        log.info("Webhook: Task created - {} for memo {}", event.getTaskId(), event.getMemoId());

        // Create MemoTask record
        MemoTask task = taskService.onTaskCreated(
                event.getTaskId(),
                event.getMemoId(),
                event.getTaskDefinitionKey(),
                event.getTaskName(),
                event.getCandidateGroups(),
                event.getCandidateUsers());

        // Resolve assignment using memo-service logic
        AssignmentService.AssignmentResult assignment = assignmentService.resolveAssignment(
                event.getMemoId(),
                event.getTaskDefinitionKey(),
                event.getProcessVariables());

        TaskCreatedResponse response = new TaskCreatedResponse();
        response.setMemoTaskId(task.getId());
        response.setAssignee(assignment.getAssignee());
        response.setCandidateGroups(assignment.getCandidateGroups());
        response.setCandidateUsers(assignment.getCandidateUsers());

        return ResponseEntity.ok(response);
    }

    /**
     * Called by workflow-service when a task is completed.
     */
    @PostMapping("/task-completed")
    public ResponseEntity<Void> onTaskCompleted(@RequestBody TaskCompletedEvent event) {
        log.info("Webhook: Task completed - {}", event.getTaskId());
        // Task completion is handled by TaskController.completeTask
        // This is just for notification purposes if needed
        return ResponseEntity.ok().build();
    }

    /**
     * Called by workflow-service when a process is completed.
     */
    @PostMapping("/process-completed")
    public ResponseEntity<Void> onProcessCompleted(@RequestBody ProcessCompletedEvent event) {
        log.info("Webhook: Process completed - {} status: {}", event.getProcessInstanceId(), event.getOutcome());
        // Update memo final status
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
        private Map<String, Object> processVariables;
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
        private String outcome; // APPROVED, REJECTED
    }

    @Data
    public static class TaskCreatedResponse {
        private UUID memoTaskId;
        private String assignee;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
    }
}
