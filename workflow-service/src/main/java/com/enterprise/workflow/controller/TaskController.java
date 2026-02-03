package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.service.WorkflowTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for task management.
 * Used for task inbox, claiming, completing, and delegating tasks.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final WorkflowTaskService taskService;

    /**
     * Get tasks assigned to current user.
     */
    @GetMapping("/assigned")
    public ResponseEntity<List<TaskDTO>> getAssignedTasks(
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId) {

        return ResponseEntity.ok(taskService.getAssignedTasks(userId));
    }

    /**
     * Get tasks assigned to current user (alias for /assigned, for frontend
     * compatibility).
     */
    @GetMapping("/my")
    public ResponseEntity<List<TaskDTO>> getMyTasks(
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId) {

        return ResponseEntity.ok(taskService.getAssignedTasks(userId));
    }

    /**
     * Get tasks that current user can claim.
     */
    @GetMapping("/claimable")
    public ResponseEntity<List<TaskDTO>> getClaimableTasks(
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        List<String> roleList = roles != null
                ? Arrays.asList(roles.split(","))
                : List.of();

        return ResponseEntity.ok(taskService.getCandidateTasks(userId, roleList));
    }

    /**
     * Get all tasks for current user (assigned + claimable).
     */
    @GetMapping("/inbox")
    public ResponseEntity<List<TaskDTO>> getInbox(
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        List<TaskDTO> assigned = taskService.getAssignedTasks(userId);
        List<String> roleList = roles != null
                ? Arrays.asList(roles.split(","))
                : List.of();
        List<TaskDTO> claimable = taskService.getCandidateTasks(userId, roleList);

        // Combine and return
        List<TaskDTO> inbox = new java.util.ArrayList<>(assigned);
        inbox.addAll(claimable);
        return ResponseEntity.ok(inbox);
    }

    /**
     * Get tasks by product (for product-specific inbox).
     */
    @GetMapping("/by-product")
    public ResponseEntity<List<TaskDTO>> getTasksByProduct(
            @RequestParam UUID productId,
            @RequestHeader(value = "X-User-Id", required = false) String assignee) {

        return ResponseEntity.ok(taskService.getTasksByProduct(productId, assignee));
    }

    /**
     * Get tasks for a specific process instance.
     */
    @GetMapping("/by-process/{processInstanceId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProcess(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(taskService.getTasksByProcessInstance(processInstanceId));
    }

    // ==================== PARALLEL EXECUTION TRACKING ====================

    /**
     * Get parallel execution status for a process instance.
     * Returns comprehensive info about parallel branches, active tasks, and
     * completion status.
     * Supports deep nested parallel gateways.
     */
    @GetMapping("/parallel-status/{processInstanceId}")
    public ResponseEntity<ParallelExecutionStatusDTO> getParallelExecutionStatus(
            @PathVariable String processInstanceId) {
        return ResponseEntity.ok(taskService.getParallelExecutionStatus(processInstanceId));
    }

    /**
     * Get all active executions (tokens) for a process instance.
     * Useful for visualizing parallel branches in UI.
     */
    @GetMapping("/executions/{processInstanceId}")
    public ResponseEntity<List<ExecutionDTO>> getActiveExecutions(
            @PathVariable String processInstanceId) {
        return ResponseEntity.ok(taskService.getActiveExecutions(processInstanceId));
    }

    /**
     * Get valid return points for a task.
     */
    @GetMapping("/{taskId}/return-points")
    public ResponseEntity<List<org.flowable.task.api.history.HistoricTaskInstance>> getReturnPoints(
            @PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getReturnableTasks(taskId));
    }

    /**
     * Send back (reject) a task.
     */
    @PostMapping("/{taskId}/send-back")
    public ResponseEntity<Void> sendBackTask(
            @PathVariable String taskId,
            @RequestBody SendBackRequest request,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "Unknown") String userName) {

        taskService.sendBackTask(taskId, request.getTargetActivityId(), request.getReason(), userId, userName);
        return ResponseEntity.ok().build();
    }

    /**
     * Get a specific task by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    /**
     * Claim a task.
     */
    @PostMapping("/{id}/claim")
    public ResponseEntity<TaskDTO> claimTask(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        return ResponseEntity.ok(taskService.claimTask(id, userId, userName));
    }

    /**
     * Unclaim (release) a task back to the pool.
     */
    @PostMapping("/{id}/unclaim")
    public ResponseEntity<Void> unclaimTask(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id") String userId) {

        taskService.unclaimTask(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a task.
     * 
     * @param cancelOthers If true, cancels other parallel tasks (for "first
     *                     approval wins" mode)
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable String id,
            @Valid @RequestBody CompleteTaskRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean cancelOthers,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        List<String> roleList = roles != null
                ? Arrays.asList(roles.split(","))
                : List.of();

        if (cancelOthers) {
            taskService.completeTaskWithCancellation(id, request, UUID.fromString(userId), userName, roleList, true);
        } else {
            taskService.completeTask(id, request, UUID.fromString(userId), userName, roleList);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Delegate a task to another user.
     */
    @PostMapping("/{id}/delegate")
    public ResponseEntity<Void> delegateTask(
            @PathVariable String id,
            @RequestParam String delegateTo,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        taskService.delegateTask(id, delegateTo, UUID.fromString(userId), userName);
        return ResponseEntity.ok().build();
    }

    /**
     * Set task priority.
     */
    @PutMapping("/{id}/priority")
    public ResponseEntity<Void> setTaskPriority(
            @PathVariable String id,
            @RequestParam int priority) {

        taskService.setTaskPriority(id, priority);
        return ResponseEntity.ok().build();
    }

    // ==================== COMMITTEE VOTING ====================

    private final com.enterprise.workflow.service.CommitteeVotingService committeeVotingService;

    /**
     * Cast a vote on a committee task.
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<CommitteeVoteDTO> castVote(
            @PathVariable String id,
            @RequestBody VoteRequest request,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        CommitteeVoteDTO result = committeeVotingService.castVote(
                id,
                userId,
                userName != null ? userName : "Unknown",
                request.getDecision(),
                request.getComment());

        return ResponseEntity.ok(result);
    }

    /**
     * Get voting status for a committee task.
     */
    @GetMapping("/{id}/vote-status")
    public ResponseEntity<CommitteeVoteDTO> getVoteStatus(@PathVariable String id) {
        return ResponseEntity.ok(committeeVotingService.getVoteStatus(id));
    }

    /**
     * Vote request DTO.
     */
    @lombok.Data
    public static class VoteRequest {
        private String decision; // APPROVE, REJECT, ABSTAIN
        private String comment;
    }
}
