package com.enterprise.memo.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.memo.dto.MemoTaskDTO;
import com.enterprise.memo.dto.TaskActionRequest;
import com.enterprise.memo.entity.MemoTask;
import com.enterprise.memo.service.MemoTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final com.enterprise.memo.repository.MemoRepository memoRepository;

    /**
     * Get user's inbox (tasks assigned to them or their groups).
     * Proxies to workflow-service and enriches with memo details
     * (subject, reference number, topic name).
     */
    @GetMapping("/inbox")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getInbox() {
        log.debug("Getting inbox for user - headers auto-propagated via WebClient filter");
        java.util.List<java.util.Map<String, Object>> tasks = workflowClient.getTaskInbox();

        // Enrich with memo details (subject, memoNumber, topicName)
        if (tasks != null && !tasks.isEmpty()) {
            // Collect all businessKeys (memo UUIDs) for batch lookup
            java.util.Set<UUID> memoIds = new java.util.HashSet<>();
            for (var task : tasks) {
                Object bk = task.get("businessKey");
                if (bk != null) {
                    try {
                        memoIds.add(UUID.fromString(bk.toString()));
                    } catch (Exception ignored) {
                    }
                }
            }

            if (!memoIds.isEmpty()) {
                // Batch lookup memos with topics eagerly loaded
                java.util.Map<UUID, com.enterprise.memo.entity.Memo> memoMap = new java.util.HashMap<>();
                try {
                    var memos = memoRepository.findAllById(memoIds);
                    for (var memo : memos) {
                        memoMap.put(memo.getId(), memo);
                    }
                } catch (Exception e) {
                    log.warn("Failed to enrich inbox with memo details: {}", e.getMessage());
                }

                // Inject memo fields into each task
                for (var task : tasks) {
                    Object bk = task.get("businessKey");
                    if (bk != null) {
                        try {
                            UUID memoId = UUID.fromString(bk.toString());
                            var memo = memoMap.get(memoId);
                            if (memo != null) {
                                // Make task map mutable if needed
                                java.util.Map<String, Object> enriched = new java.util.HashMap<>(task);
                                enriched.put("subject", memo.getSubject());
                                enriched.put("memoNumber", memo.getMemoNumber());
                                try {
                                    enriched.put("topicName", memo.getTopic().getName());
                                } catch (Exception e) {
                                    enriched.put("topicName", null);
                                }
                                task.clear();
                                task.putAll(enriched);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

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
    public ResponseEntity<Void> claimTask(@PathVariable String taskId) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName() != null ? user.getName() : "Anonymous User";

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
            @RequestBody TaskActionRequest request) {

        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        String userName = user.getName() != null ? user.getName() : "Anonymous User";

        log.info("Completing task: {} with action: {} by user: {}", taskId, request.getAction(), userId);

        // Convert TaskActionRequest to variables map for workflow-service
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        variables.put("decision", request.getAction());
        variables.put("comment", request.getComment());

        // The action sent from frontend is now the actionType (e.g., "REJECT"),
        // not the label (e.g., "Reject Memo"). This is the stable identifier.
        String actionType = request.getAction();

        // Enterprise pattern: resolve outcome config and inject option.sets variables
        try {
            java.util.Map<String, Object> outcomeConfig = workflowClient.getOutcomeConfig(taskId);
            if (outcomeConfig != null && outcomeConfig.get("options") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> options = (java.util.List<java.util.Map<String, Object>>) outcomeConfig
                        .get("options");

                // Find the option whose actionType matches
                for (java.util.Map<String, Object> option : options) {
                    String optActionType = (String) option.get("actionType");
                    if (optActionType != null && optActionType.equalsIgnoreCase(actionType)) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> sets = (java.util.Map<String, Object>) option.get("sets");
                        if (sets != null && !sets.isEmpty()) {
                            variables.putAll(sets);
                            log.info("Injected sets from outcome option (actionType={}): {}", optActionType, sets);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve outcome config for task {}: {}", taskId, e.getMessage());
        }

        // Check if we should cancel other parallel tasks and get the gateway ID
        var cancellationInfo = getCancellationInfo(taskId);
        boolean cancelOthers = cancellationInfo.shouldCancel();
        String gatewayId = cancellationInfo.gatewayId();

        log.info("Task {} cancelOthers={} gatewayId={}", taskId, cancelOthers, gatewayId);

        workflowClient.completeTask(taskId, userId, userName, variables,
                actionType, cancelOthers, gatewayId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get outcome configuration for a task.
     * Returns the configured options with their sets maps for the UI to
     * render dynamic buttons.
     */
    @GetMapping("/{taskId}/outcome-config")
    public ResponseEntity<java.util.Map<String, Object>> getOutcomeConfig(@PathVariable String taskId) {
        log.debug("Getting outcome config for task: {}", taskId);
        java.util.Map<String, Object> config = workflowClient.getOutcomeConfig(taskId);
        if (config == null || config.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * Get delegate candidates for a task.
     * Returns candidate groups and candidate users for the delegate user picker.
     */
    @GetMapping("/{taskId}/delegate-candidates")
    public ResponseEntity<java.util.Map<String, Object>> getDelegateCandidates(@PathVariable String taskId) {
        log.debug("Getting delegate candidates for task: {}", taskId);
        java.util.Map<String, Object> candidates = workflowClient.getDelegateCandidates(taskId);
        return ResponseEntity.ok(candidates);
    }

    /**
     * Info about whether to cancel other tasks and which gateway scope to use.
     */
    private record CancellationInfo(boolean shouldCancel, String gatewayId) {
    }

    /**
     * Determine if completing this task should cancel other parallel tasks.
     * Returns the gateway ID for scoped cancellation (supports nested gateways).
     * Based on gateway configuration: completion_mode=ANY and cancel_remaining=true
     */
    private CancellationInfo getCancellationInfo(String taskId) {
        try {
            // Get the MemoTask to find the memo and topic
            return taskService.getByWorkflowTaskId(taskId)
                    .map(memoTask -> {
                        UUID topicId = memoTask.getMemo().getTopic().getId();
                        // Check all gateways for this topic
                        var configs = gatewayConfigService.getConfigsForTopic(topicId);

                        // Find the first gateway configured with ANY mode and cancelRemaining
                        for (var config : configs) {
                            if (config
                                    .getCompletionMode() == com.enterprise.memo.entity.WorkflowGatewayConfig.CompletionMode.ANY
                                    && Boolean.TRUE.equals(config.getCancelRemaining())) {
                                return new CancellationInfo(true, config.getGatewayId());
                            }
                        }
                        return new CancellationInfo(false, null);
                    })
                    .orElse(new CancellationInfo(false, null));
        } catch (Exception e) {
            log.warn("Failed to check gateway config for task {}: {}", taskId, e.getMessage());
            return new CancellationInfo(false, null);
        }
    }

    /**
     * Get tasks for a specific memo.
     * Filters to only show PENDING tasks that the current user can act on.
     * Includes inline outcomeConfig for each task to eliminate a separate API
     * call.
     */
    @GetMapping("/memo/{memoId}")
    public ResponseEntity<List<MemoTaskDTO>> getTasksForMemo(@PathVariable UUID memoId) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        // Use role IDs (UUIDs) for candidate groups matching - candidateGroups should
        // store role IDs
        List<String> userGroups = new ArrayList<>(user.getRoleIds());

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
                .map(task -> {
                    MemoTaskDTO dto = MemoTaskDTO.fromEntity(task);
                    // Inline the outcomeConfig to eliminate separate API call from frontend
                    try {
                        if (task.getWorkflowTaskId() != null) {
                            java.util.Map<String, Object> outcomeConfig = workflowClient
                                    .getOutcomeConfig(task.getWorkflowTaskId());
                            dto.setOutcomeConfig(outcomeConfig);
                        }
                    } catch (Exception e) {
                        log.debug("Could not resolve outcomeConfig for task {}: {}",
                                task.getWorkflowTaskId(), e.getMessage());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        log.debug("Returning {} filtered tasks", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get movement history for a task.
     * Proxies to workflow-service. Returns history entries and valid return
     * points.
     */
    @GetMapping("/{taskId}/movement-history")
    public ResponseEntity<java.util.Map<String, Object>> getMovementHistory(@PathVariable String taskId) {
        log.debug("Getting movement history for task: {}", taskId);
        java.util.Map<String, Object> history = workflowClient.getMovementHistory(taskId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(history);
    }

    /**
     * Get valid return points for send-back.
     * Proxies to workflow-service. Returns list of steps this task can be sent
     * back to.
     */
    @GetMapping("/{taskId}/return-points")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getReturnPoints(
            @PathVariable String taskId) {
        log.debug("Getting return points for task: {}", taskId);
        java.util.List<java.util.Map<String, Object>> points = workflowClient.getReturnPoints(taskId);
        return ResponseEntity.ok(points);
    }

    /**
     * Send back a task to a previous step.
     * Proxies to workflow-service.
     */
    @PostMapping("/{taskId}/send-back")
    public ResponseEntity<Void> sendBackTask(
            @PathVariable String taskId,
            @RequestBody java.util.Map<String, String> request) {
        log.debug("Sending back task {} to {}", taskId, request.get("targetActivityId"));
        workflowClient.sendBackTask(taskId, request.get("targetActivityId"), request.get("reason"));
        return ResponseEntity.ok().build();
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

        // User is in candidate users (parse comma-separated string)
        String candidateUsers = task.getCandidateUsers();
        if (candidateUsers != null && !candidateUsers.isBlank()) {
            Set<String> candidateUserSet = Set.of(candidateUsers.split(","));
            if (candidateUserSet.contains(userId)) {
                return true;
            }
        }

        // User's groups match candidate groups (parse comma-separated string)
        String candidateGroups = task.getCandidateGroups();
        if (candidateGroups != null && !candidateGroups.isBlank() && userGroups != null) {
            Set<String> candidateGroupSet = Set.of(candidateGroups.split(","));
            for (String group : userGroups) {
                if (candidateGroupSet.contains(group.trim())) {
                    log.debug("User group {} matches candidate group in {}", group, candidateGroups);
                    return true;
                }
            }
        }

        return false;
    }

}
