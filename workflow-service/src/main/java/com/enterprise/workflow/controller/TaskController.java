package com.enterprise.workflow.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.service.WorkflowTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final com.enterprise.workflow.service.MovementHistoryService movementHistoryService;

    /**
     * Get tasks assigned to current user.
     */
    @GetMapping("/assigned")
    public ResponseEntity<List<TaskDTO>> getAssignedTasks() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(taskService.getAssignedTasks(user.getUserId()));
    }

    /**
     * Get tasks assigned to current user (alias for /assigned, for frontend
     * compatibility).
     */
    @GetMapping("/my")
    public ResponseEntity<List<TaskDTO>> getMyTasks() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(taskService.getAssignedTasks(user.getUserId()));
    }

    /**
     * Get tasks that current user can claim.
     */
    @GetMapping("/claimable")
    public ResponseEntity<List<TaskDTO>> getClaimableTasks() {
        UserContext user = UserContextHolder.require();
        List<String> roleList = new ArrayList<>(user.getRoles());
        return ResponseEntity.ok(taskService.getCandidateTasks(user.getUserId(), roleList));
    }

    /**
     * Get all tasks for current user (assigned + claimable) with pagination.
     *
     * @param page     page number (0-based, default 0)
     * @param size     items per page (default 10)
     * @param sortBy   sort field: createTime, priority, name (default createTime)
     * @param sortDir  sort direction: asc, desc (default desc)
     * @param priority filter by priority: NORMAL, HIGH, URGENT, ALL
     * @param search   search term (matches task name, process title)
     */
    @GetMapping("/inbox")
    public ResponseEntity<com.cas.common.dto.ApiResponse<com.cas.common.dto.PagedData<TaskDTO>>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {

        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        List<String> roleList = new ArrayList<>(user.getRoleIds());

        com.cas.common.dto.PagedData<TaskDTO> pagedData = taskService.getInboxPaged(
                userId, roleList, page, size, sortBy, sortDir, priority, search);

        return ResponseEntity.ok(com.cas.common.dto.ApiResponse.success(pagedData));
    }

    /**
     * Get tasks by product (for product-specific inbox).
     */
    @GetMapping("/by-product")
    public ResponseEntity<List<TaskDTO>> getTasksByProduct(@RequestParam UUID productId) {
        UserContext user = UserContextHolder.getContext();
        String assignee = user != null ? user.getUserId() : null;
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

    // ==================== MOVEMENT HISTORY & BACK NAVIGATION ====================

    /**
     * Get movement history and valid return points for a task.
     * Uses ActionTimeline events instead of static BPMN paths, so it correctly
     * handles loops, send-backs, and parallel branches.
     */
    @GetMapping("/{taskId}/movement-history")
    public ResponseEntity<com.enterprise.workflow.dto.MovementHistoryDTO> getMovementHistory(
            @PathVariable String taskId) {
        return ResponseEntity.ok(movementHistoryService.getMovementHistory(taskId));
    }

    /**
     * Get valid return points for a task (backward compatible alias).
     * Returns simplified list built from movement history.
     */
    @GetMapping("/{taskId}/return-points")
    public ResponseEntity<List<com.enterprise.workflow.dto.MovementHistoryDTO.ReturnPoint>> getReturnPoints(
            @PathVariable String taskId) {
        com.enterprise.workflow.dto.MovementHistoryDTO history = movementHistoryService.getMovementHistory(taskId);
        return ResponseEntity.ok(history.getReturnPoints());
    }

    /**
     * Send back (reject) a task.
     */
    @PostMapping("/{taskId}/send-back")
    public ResponseEntity<Void> sendBackTask(
            @PathVariable String taskId,
            @RequestBody SendBackRequest request) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName() != null ? user.getName() : "Unknown";

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
    public ResponseEntity<TaskDTO> claimTask(@PathVariable String id) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName();

        return ResponseEntity.ok(taskService.claimTask(id, userId, userName));
    }

    /**
     * Unclaim (release) a task back to the pool.
     */
    @PostMapping("/{id}/unclaim")
    public ResponseEntity<Void> unclaimTask(@PathVariable String id) {
        UserContext user = UserContextHolder.require();
        taskService.unclaimTask(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a task.
     * 
     * @param cancelOthers If true, cancels other parallel tasks (for "first
     *                     approval wins" mode)
     * @param gatewayId    Optional. If provided with cancelOthers=true, only
     *                     cancels tasks
     *                     within this gateway's scope. This is required for nested
     *                     gateway support.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable String id,
            @Valid @RequestBody CompleteTaskRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean cancelOthers,
            @RequestParam(required = false) String gatewayId) {

        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName();
        List<String> roleList = new ArrayList<>(user.getRoles());

        if (cancelOthers) {
            if (gatewayId != null && !gatewayId.isEmpty()) {
                // Gateway-scoped cancellation (preferred for nested gateway support)
                taskService.completeTaskWithGatewayScopedCancellation(
                        id, request, UUID.fromString(userId), userName, roleList, gatewayId);
            } else {
                // Legacy: cancel ALL other tasks (doesn't support nested gateways well)
                taskService.completeTaskWithCancellation(id, request, UUID.fromString(userId), userName, roleList,
                        true);
            }
        } else {
            taskService.completeTask(id, request, UUID.fromString(userId), userName, roleList);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Get eligible delegate candidates for a task.
     * Returns candidate groups and candidate users from the Flowable task.
     */
    @GetMapping("/{id}/delegate-candidates")
    public ResponseEntity<java.util.Map<String, Object>> getDelegateCandidates(@PathVariable String id) {
        var candidates = taskService.getDelegateCandidates(id);
        return ResponseEntity.ok(candidates);
    }

    /**
     * Delegate a task to another user.
     */
    @PostMapping("/{id}/delegate")
    public ResponseEntity<Void> delegateTask(
            @PathVariable String id,
            @RequestParam String delegateTo) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName();

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
            @RequestBody VoteRequest request) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName() != null ? user.getName() : "Unknown";

        CommitteeVoteDTO result = committeeVotingService.castVote(
                id,
                userId,
                userName,
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
