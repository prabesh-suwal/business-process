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

        workflowClient.completeTask(taskId, userId, userName, variables);
        return ResponseEntity.ok().build();
    }

    /**
     * Get tasks for a specific memo.
     */
    @GetMapping("/memo/{memoId}")
    public ResponseEntity<List<MemoTaskDTO>> getTasksForMemo(@PathVariable UUID memoId) {
        List<MemoTask> tasks = taskService.getTasksForMemo(memoId);
        List<MemoTaskDTO> dtos = tasks.stream()
                .map(MemoTaskDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Helper methods to extract user info from request headers (set by gateway)
    private String getUserId(HttpServletRequest request) {
        // Debug: Log all headers
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        log.debug("=== Request Headers ===");
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("  {}: {}", headerName, request.getHeader(headerName));
        }
        log.debug("=== End Headers ===");

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
        String groups = request.getHeader("X-User-Groups");
        if (groups != null && !groups.isBlank()) {
            return List.of(groups.split(","));
        }
        return List.of();
    }
}
