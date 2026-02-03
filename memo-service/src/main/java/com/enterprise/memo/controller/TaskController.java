package com.enterprise.memo.controller;

import com.enterprise.memo.dto.MemoTaskDTO;
import com.enterprise.memo.dto.TaskActionRequest;
import com.enterprise.memo.entity.MemoTask;
import com.enterprise.memo.service.MemoTaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Task management endpoints in memo-service.
 * This is the ONLY task API the UI should call.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final MemoTaskService taskService;
    private final com.enterprise.memo.client.WorkflowClient workflowClient;
    private final com.enterprise.memo.service.GatewayConfigService gatewayConfigService;

    /**
     * Get user's inbox (tasks assigned to them or their groups).
     * Proxies to workflow-service.
     */
    @GetMapping("/inbox")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getInbox(HttpServletRequest request) {
        String userId = getUserId(request);
        String roles = request.getHeader("X-User-Roles");

        log.debug("Getting inbox for user: {} with roles: {}", userId, roles);
        java.util.List<java.util.Map<String, Object>> tasks = workflowClient.getTaskInbox(userId, roles);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get a specific task.
     * Proxies to workflow-service.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<java.util.Map<String, Object>> getTask(@PathVariable String taskId) {
        log.debug("Getting task: {}", taskId);
        java.util.Map<String, Object> task = workflowClient.getTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * Claim a task.
     * Proxies to workflow-service.
     */
    @PostMapping("/{taskId}/claim")
    public ResponseEntity<Void> claimTask(
            @PathVariable String taskId,
            HttpServletRequest request) {
        String userId = getUserId(request);
        String userName = getUserName(request);

        log.info("Claiming task: {} for user: {}", taskId, userId);
        workflowClient.claimTask(taskId, userId, userName);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a task with action (approve, reject, etc.).
     * Proxies to workflow-service.
     * Checks gateway configuration to determine if other parallel tasks should be
     * cancelled.
     */
    @PostMapping("/{taskId}/action")
    public ResponseEntity<Void> completeTask(
            @PathVariable String taskId,
            @RequestBody TaskActionRequest request,
            HttpServletRequest httpRequest) {

        String userId = getUserId(httpRequest);
        String userName = getUserName(httpRequest);

        log.info("Completing task: {} with action: {} by user: {}", taskId, request.getAction(), userId);

        // Convert TaskActionRequest to variables map for workflow-service
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        variables.put("decision", request.getAction());
        variables.put("comment", request.getComment());

        // Check if we should cancel other parallel tasks
        boolean cancelOthers = shouldCancelOtherTasks(taskId);
        log.info("Task {} cancelOthers={}", taskId, cancelOthers);

        workflowClient.completeTask(taskId, userId, userName, variables, cancelOthers);
        return ResponseEntity.ok().build();
    }

    /**
     * Determine if completing this task should cancel other parallel tasks.
     * Based on gateway configuration: completion_mode=ANY and cancel_remaining=true
     */
    private boolean shouldCancelOtherTasks(String taskId) {
        try {
            // Get the MemoTask to find the memo and topic
            return taskService.getByWorkflowTaskId(taskId)
                    .map(memoTask -> {
                        UUID topicId = memoTask.getMemo().getTopic().getId();
                        // Check all gateways for this topic
                        var configs = gatewayConfigService.getConfigsForTopic(topicId);
                        return configs.stream().anyMatch(config -> config
                                .getCompletionMode() == com.enterprise.memo.entity.WorkflowGatewayConfig.CompletionMode.ANY
                                && Boolean.TRUE.equals(config.getCancelRemaining()));
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check gateway config for task {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    /**
     * Get tasks for a specific memo.
     * Filters to only show PENDING tasks that the current user can act on.
     */
    @GetMapping("/memo/{memoId}")
    public ResponseEntity<List<MemoTaskDTO>> getTasksForMemo(
            @PathVariable UUID memoId,
            HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        List<String> userGroups = getUserGroups(httpRequest);

        log.debug("getTasksForMemo: userId={}, userGroups={}", userId, userGroups);

        List<MemoTask> tasks = taskService.getTasksForMemo(memoId);
        log.debug("Found {} tasks for memo {}", tasks.size(), memoId);

        // Filter to only PENDING tasks that the user can act on
        List<MemoTaskDTO> dtos = tasks.stream()
                .filter(task -> {
                    boolean isPending = task.getStatus() == MemoTask.TaskStatus.PENDING;
                    log.debug("Task {} ({}) status={} isPending={}", task.getTaskName(), task.getCandidateGroups(),
                            task.getStatus(), isPending);
                    return isPending;
                })
                .filter(task -> {
                    boolean canAct = canUserActOnTask(task, userId, userGroups);
                    log.debug("Task {} canAct={} (candidateGroups={}, userGroups={})", task.getTaskName(), canAct,
                            task.getCandidateGroups(), userGroups);
                    return canAct;
                })
                .map(MemoTaskDTO::fromEntity)
                .collect(Collectors.toList());

        log.debug("Returning {} filtered tasks", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Check if user can act on a task based on assignment or candidate
     * groups/users.
     */
    private boolean canUserActOnTask(MemoTask task, String userId, List<String> userGroups) {
        // Assigned directly to user
        if (userId.equals(task.getAssignedTo())) {
            return true;
        }

        // User is in candidate users
        if (task.getCandidateUsers() != null && task.getCandidateUsers().contains(userId)) {
            return true;
        }

        // User's groups match candidate groups
        if (task.getCandidateGroups() != null && userGroups != null) {
            for (String group : userGroups) {
                if (task.getCandidateGroups().contains(group)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Helper methods to extract user info from request headers (set by gateway)
    private String getUserId(HttpServletRequest request) {
        // Debug: Log all headers
        // java.util.Enumeration<String> headerNames = request.getHeaderNames();
        // log.debug("=== Request Headers ===");
        // while (headerNames.hasMoreElements()) {
        // String headerName = headerNames.nextElement();
        // log.debug(" {}: {}", headerName, request.getHeader(headerName));
        // }
        // log.debug("=== End Headers ===");

        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("X-User-Id header is required but was not provided by gateway");
        }
        return userId;
    }

    private String getUserName(HttpServletRequest request) {
        String name = request.getHeader("X-User-Name");
        return name != null ? name : "Anonymous User";
    }

    private List<String> getUserGroups(HttpServletRequest request) {
        // Gateway sends X-User-Roles header with comma-separated roles
        String roles = request.getHeader("X-User-Roles");
        if (roles != null && !roles.isBlank()) {
            return List.of(roles.split(","));
        }
        return List.of();
    }
}
