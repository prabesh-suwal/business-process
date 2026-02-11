package com.enterprise.workflow.service;

import com.cas.common.logging.audit.AuditEventType;
import com.cas.common.logging.audit.AuditLogger;
import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.ProcessInstanceMetadata;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.ProcessTemplateForm;
import com.enterprise.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing tasks (inbox, complete, claim, delegate).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowTaskService {

    private final TaskService taskService;
    private final org.flowable.engine.RuntimeService runtimeService;
    private final org.flowable.engine.HistoryService historyService;
    private final ProcessInstanceMetadataRepository processInstanceMetadataRepository;
    private final ProcessTemplateFormRepository processTemplateFormRepository;
    private final ActionTimelineRepository actionTimelineRepository;
    private final VariableAuditRepository variableAuditRepository;
    private final WebClient.Builder webClientBuilder;
    @Lazy
    private final ProcessAnalyzerService processAnalyzerService;
    private final AuditLogger auditLogger;

    @Value("${memo.service.url:http://localhost:9008}")
    private String memoServiceUrl;

    /**
     * Get tasks assigned to a specific user.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getAssignedTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks where user is a candidate (can claim).
     * For MVP: if no roles provided, returns ALL unassigned tasks.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getCandidateTasks(String userId, List<String> groups) {
        TaskQuery query = taskService.createTaskQuery()
                .taskUnassigned();

        // If groups provided, match EITHER candidate user OR candidate group
        if (groups != null && !groups.isEmpty()) {
            query.or()
                    .taskCandidateUser(userId)
                    .taskCandidateGroupIn(groups)
                    .endOr();
        } else {
            // For MVP: if no roles/groups contexts, see all unassigned tasks (Admin-like
            // behavior)
            // Or if we want to support users with no roles seeing their direct tasks:
            query.or()
                    .taskCandidateUser(userId)
                    // If we want "admin see all", we can't restrict.
                    // But current logic was "no groups -> all unassigned".
                    // Let's keep "no groups -> all unassigned" for backward compat if that's the
                    // intent.
                    // But usually userId is always present.
                    .endOr();

            // Actually, the previous logic was: if groups empty, do NOTHING to query ->
            // returns ALL unassigned.
            // Let's preserve that but making sure we don't break "Specific People" for
            // users WITHOUT roles (unlikely).
            // BUT, strictly speaking, if groups is empty, we probably just want all
            // unassigned.
            // Let's stick to the IF block for the restricted case.
        }

        List<Task> tasks = query
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks for a specific process instance.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProcessInstance(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks for a product (all tasks from processes of that product).
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProduct(UUID productId, String assignee) {
        // Get all process instances for this product
        List<ProcessInstanceMetadata> instances = processInstanceMetadataRepository
                .findByProductIdOrderByStartedAtDesc(productId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (instances.isEmpty()) {
            return List.of();
        }

        List<String> processInstanceIds = instances.stream()
                .map(ProcessInstanceMetadata::getFlowableProcessInstanceId)
                .toList();

        TaskQuery query = taskService.createTaskQuery()
                .processInstanceIdIn(processInstanceIds);

        if (assignee != null) {
            query.taskAssignee(assignee);
        }

        return query.orderByTaskCreateTime().desc().list().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get a specific task by ID.
     */
    @Transactional(readOnly = true)
    public TaskDTO getTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        return toDTO(task);
    }

    /**
     * Claim a task from the pool.
     */
    public TaskDTO claimTask(String taskId, String userId, String userName) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (task.getAssignee() != null) {
            throw new IllegalStateException("Task is already assigned to: " + task.getAssignee());
        }

        taskService.claim(taskId, userId);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_CLAIMED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(UUID.fromString(userId))
                .actorName(userName)
                .build();
        actionTimelineRepository.save(timelineEvent);

        // Audit log
        auditLogger.log()
                .eventType(AuditEventType.CLAIM)
                .action("Claimed workflow task")
                .module("WORKFLOW")
                .entity("TASK", taskId)
                .remarks("Task: " + task.getName())
                .success();

        log.info("Task {} claimed by user {}", taskId, userName);

        return getTask(taskId);
    }

    /**
     * Unclaim (release) a task back to the pool.
     */
    public void unclaimTask(String taskId, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Release the task (set assignee to null)
        taskService.unclaim(taskId);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_CLAIMED) // Reusing for unclaim event
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(UUID.fromString(userId))
                .metadata(Map.of("action", "unclaimed"))
                .build();
        actionTimelineRepository.save(timelineEvent);

        // Audit log
        auditLogger.log()
                .eventType(AuditEventType.UNCLAIM)
                .action("Released workflow task back to pool")
                .module("WORKFLOW")
                .entity("TASK", taskId)
                .remarks("Task: " + task.getName())
                .success();

        log.info("Task {} unclaimed by user {}", taskId, userId);
    }

    /**
     * Complete a task with variables.
     */
    public void completeTask(String taskId, CompleteTaskRequest request, UUID userId, String userName,
            List<String> userRoles) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Add comment if provided
        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment());
        }

        // Build variables map — always include 'outcome' derived from approved flag
        // so BPMN gateways using ${outcome == 'approved'} / ${outcome == 'rejected'}
        // work
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("outcome", request.isApproved() ? "approved" : "rejected");
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            variables.putAll(request.getVariables());
        }
        taskService.complete(taskId, variables);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_COMPLETED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(userId)
                .actorName(userName)
                .actorRoles(userRoles)
                .metadata(Map.of(
                        "approved", request.isApproved(),
                        "hasComment", request.getComment() != null))
                .build();
        actionTimelineRepository.save(timelineEvent);

        // Audit log
        auditLogger.log()
                .eventType(AuditEventType.COMPLETE)
                .action("Completed workflow task")
                .module("WORKFLOW")
                .entity("TASK", taskId)
                .remarks("Task: " + task.getName() + ", Approved: " + request.isApproved())
                .success();

        // Notify memo-service of task completion
        notifyMemoServiceTaskCompleted(taskId, request.isApproved() ? "APPROVE" : "REJECT", userName);

        log.info("Task {} completed by user {}. Approved: {}", taskId, userName, request.isApproved());
    }

    /**
     * Cancel remaining sibling tasks in the same parallel branch.
     * Used for "first approval wins" (ANY mode) where completing one branch cancels
     * others.
     * 
     * Instead of deleting tasks (which Flowable doesn't allow for running
     * processes),
     * we complete them with a "skipped" status so the parallel join can proceed.
     * 
     * @param processInstanceId   The process instance containing parallel tasks
     * @param completedTaskDefKey The task definition key that was just completed
     * @param cancelReason        Reason for cancellation (shown in audit)
     * @return List of cancelled task IDs
     */
    public List<String> cancelSiblingParallelTasks(String processInstanceId, String completedTaskDefKey,
            String cancelReason, UUID cancelledBy, String cancelledByName) {

        // Get all active tasks in this process instance
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();

        List<String> cancelledTaskIds = new java.util.ArrayList<>();

        for (Task siblingTask : activeTasks) {
            // Skip if it's the same task definition (shouldn't happen, but safety check)
            if (siblingTask.getTaskDefinitionKey().equals(completedTaskDefKey)) {
                continue;
            }

            String taskId = siblingTask.getId();

            // Add cancellation comment
            taskService.addComment(taskId, processInstanceId,
                    "Task auto-completed (skipped): " + cancelReason + " (parallel branch completed first)");

            // Complete the task with skipped=true so parallel join can proceed
            // We can't use deleteTask() as Flowable doesn't allow deleting tasks in running
            // processes
            Map<String, Object> skipVariables = new java.util.HashMap<>();
            skipVariables.put("skipped", true);
            skipVariables.put("skipReason", cancelReason);
            skipVariables.put("skippedBy", cancelledByName);
            taskService.complete(taskId, skipVariables);

            // Record in timeline
            ActionTimeline timelineEvent = ActionTimeline.builder()
                    .processInstanceId(processInstanceId)
                    .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                    .taskId(taskId)
                    .taskName(siblingTask.getName())
                    .actorId(cancelledBy)
                    .actorName(cancelledByName)
                    .metadata(Map.of(
                            "reason", cancelReason,
                            "triggeredBy", completedTaskDefKey,
                            "skipped", true))
                    .build();
            actionTimelineRepository.save(timelineEvent);

            // Notify memo-service of task cancellation
            notifyMemoServiceTaskCompleted(taskId, "CANCELLED", cancelledByName);

            cancelledTaskIds.add(taskId);
            log.info("Skipped parallel task {} ({}) - another branch completed first",
                    taskId, siblingTask.getName());
        }

        return cancelledTaskIds;
    }

    /**
     * Cancel specific sibling tasks by their IDs.
     * This is used when we need to cancel only tasks that existed BEFORE a task was
     * completed,
     * to avoid accidentally cancelling new tasks created by gateways.
     */
    private void cancelSpecificSiblingTasks(String processInstanceId, String completedTaskDefKey,
            List<String> taskIdsToCancel, String cancelReason, UUID cancelledBy, String cancelledByName) {

        for (String taskId : taskIdsToCancel) {
            Task siblingTask = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (siblingTask == null) {
                log.warn("Task {} no longer exists, skipping cancellation", taskId);
                continue;
            }

            // Add cancellation comment
            taskService.addComment(taskId, processInstanceId,
                    "Task auto-completed (skipped): " + cancelReason + " (parallel branch completed first)");

            // Complete the task with skipped=true so parallel join can proceed
            Map<String, Object> skipVariables = new java.util.HashMap<>();
            skipVariables.put("skipped", true);
            skipVariables.put("skipReason", cancelReason);
            skipVariables.put("skippedBy", cancelledByName);
            taskService.complete(taskId, skipVariables);

            // Record in timeline
            ActionTimeline timelineEvent = ActionTimeline.builder()
                    .processInstanceId(processInstanceId)
                    .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                    .taskId(taskId)
                    .taskName(siblingTask.getName())
                    .actorId(cancelledBy)
                    .actorName(cancelledByName)
                    .metadata(Map.of(
                            "reason", cancelReason,
                            "triggeredBy", completedTaskDefKey,
                            "skipped", true))
                    .build();
            actionTimelineRepository.save(timelineEvent);

            // Notify memo-service of task cancellation
            notifyMemoServiceTaskCompleted(taskId, "CANCELLED", cancelledByName);

            log.info("Skipped parallel task {} ({}) - another branch completed first",
                    taskId, siblingTask.getName());
        }
    }

    /**
     * Complete a task with "first wins" semantics - cancels other parallel tasks.
     * Use this for workflows configured with ANY completion mode.
     * 
     * IMPORTANT: We capture sibling tasks BEFORE completing the current task,
     * because completing might trigger new tasks (e.g., after a parallel gateway).
     */
    public void completeTaskWithCancellation(String taskId, CompleteTaskRequest request,
            UUID userId, String userName, List<String> userRoles, boolean cancelOthers) {

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskDefKey = task.getTaskDefinitionKey();

        // IMPORTANT: Get sibling tasks BEFORE completing the current task!
        // This prevents us from accidentally cancelling new tasks created by parallel
        // gateways.
        List<String> siblingTaskIds = new java.util.ArrayList<>();
        if (cancelOthers) {
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            for (Task activeTask : activeTasks) {
                // Don't include the current task being completed
                if (!activeTask.getId().equals(taskId)) {
                    siblingTaskIds.add(activeTask.getId());
                }
            }
            log.info("Found {} sibling tasks to cancel: {}", siblingTaskIds.size(), siblingTaskIds);
        }

        // Now complete the current task
        completeTask(taskId, request, userId, userName, userRoles);

        // Then, if there were sibling tasks, cancel them
        if (cancelOthers && !siblingTaskIds.isEmpty()) {
            cancelSpecificSiblingTasks(processInstanceId, taskDefKey, siblingTaskIds,
                    "First approval received", userId, userName);
        }
    }

    /**
     * Complete a task with gateway-scoped "ANY" mode cancellation.
     * 
     * This is the PREFERRED method for ANY completion mode as it supports NESTED
     * GATEWAYS.
     * Only cancels tasks that are within the same gateway scope, not tasks in
     * parent
     * or child gateway scopes.
     * 
     * @param taskId    The task being completed
     * @param request   The completion request with variables
     * @param userId    User completing the task
     * @param userName  User's display name
     * @param userRoles User's roles
     * @param gatewayId The parallel gateway ID that defines the scope
     */
    public void completeTaskWithGatewayScopedCancellation(String taskId, CompleteTaskRequest request,
            UUID userId, String userName, List<String> userRoles, String gatewayId) {

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskDefKey = task.getTaskDefinitionKey();

        // Use ProcessAnalyzerService to find only sibling tasks in the same gateway
        // scope
        List<String> tasksToCancel = processAnalyzerService.getSiblingBranchTasksToCancel(
                processInstanceId, taskId, gatewayId);

        log.info("Found {} tasks to cancel in gateway {} scope: {}",
                tasksToCancel.size(), gatewayId, tasksToCancel);

        // Complete the current task first
        completeTask(taskId, request, userId, userName, userRoles);

        // Then cancel the sibling tasks in the same gateway scope
        if (!tasksToCancel.isEmpty()) {
            cancelSpecificSiblingTasks(processInstanceId, taskDefKey, tasksToCancel,
                    "Parallel branch completed (ANY mode)", userId, userName);
        }
    }

    /**
     * Cancel all tasks within a specific gateway scope.
     * Used when a branch in the gateway completes and remaining branches should be
     * cancelled.
     * 
     * @param processInstanceId The process instance ID
     * @param gatewayId         The gateway ID defining the scope
     * @param triggeringTaskId  The task that triggered the cancellation (won't be
     *                          cancelled)
     * @param cancelledBy       User who triggered the cancellation
     * @param cancelledByName   User's name
     * @return List of cancelled task IDs
     */
    public List<String> cancelTasksInGatewayScope(String processInstanceId, String gatewayId,
            String triggeringTaskId, UUID cancelledBy, String cancelledByName) {

        List<String> tasksToCancel = processAnalyzerService.getSiblingBranchTasksToCancel(
                processInstanceId, triggeringTaskId, gatewayId);

        log.info("Cancelling {} tasks in gateway {} scope", tasksToCancel.size(), gatewayId);

        for (String taskId : tasksToCancel) {
            Task siblingTask = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (siblingTask == null) {
                log.warn("Task {} no longer exists, skipping cancellation", taskId);
                continue;
            }

            // Add cancellation comment
            taskService.addComment(taskId, processInstanceId,
                    "Task auto-completed (skipped): Parallel branch " + gatewayId + " completed first");

            // Complete the task with skipped=true so parallel join can proceed
            Map<String, Object> skipVariables = new java.util.HashMap<>();
            skipVariables.put("skipped", true);
            skipVariables.put("skipReason", "Parallel branch completed first");
            skipVariables.put("skippedBy", cancelledByName);
            skipVariables.put("gatewayId", gatewayId);
            taskService.complete(taskId, skipVariables);

            // Record in timeline
            ActionTimeline timelineEvent = ActionTimeline.builder()
                    .processInstanceId(processInstanceId)
                    .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                    .taskId(taskId)
                    .taskName(siblingTask.getName())
                    .actorId(cancelledBy)
                    .actorName(cancelledByName)
                    .metadata(Map.of(
                            "reason", "Parallel branch completed first",
                            "gatewayId", gatewayId,
                            "skipped", true))
                    .build();
            actionTimelineRepository.save(timelineEvent);

            // Notify memo-service of task cancellation
            notifyMemoServiceTaskCompleted(taskId, "CANCELLED", cancelledByName);

            log.info("Cancelled task {} ({}) - gateway {} branch completed first",
                    taskId, siblingTask.getName(), gatewayId);
        }

        return tasksToCancel;
    }

    /**
     * Delegate a task to another user.
     */
    public void delegateTask(String taskId, String delegateTo, UUID delegatedBy, String delegatedByName) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        taskService.delegateTask(taskId, delegateTo);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_DELEGATED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(delegatedBy)
                .actorName(delegatedByName)
                .metadata(Map.of("delegateTo", delegateTo))
                .build();
        actionTimelineRepository.save(timelineEvent);

        // Audit log
        auditLogger.log()
                .eventType(AuditEventType.DELEGATE)
                .action("Delegated workflow task")
                .module("WORKFLOW")
                .entity("TASK", taskId)
                .remarks("Task: " + task.getName() + ", Delegated to: " + delegateTo)
                .success();

        log.info("Task {} delegated to {} by user {}", taskId, delegateTo, delegatedByName);
    }

    /**
     * Set task priority.
     */
    public void setTaskPriority(String taskId, int priority) {
        taskService.setPriority(taskId, priority);
        log.debug("Set priority {} on task {}", priority, taskId);
    }

    /**
     * Get valid return points (previous user tasks) for a task.
     */
    @Transactional(readOnly = true)
    public List<org.flowable.task.api.history.HistoricTaskInstance> getReturnableTasks(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Get completed user tasks in this process
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
    }

    /**
     * Send back (reject) a task to a previous activity or start node.
     */
    public void sendBackTask(String taskId, String targetActivityId, String reason, String userId, String userName) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String currentActivityId = task.getTaskDefinitionKey();
        log.info("Sending back task {} from {} to {}", taskId, currentActivityId, targetActivityId);

        // 1. Move token using Change Activity State
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(currentActivityId, targetActivityId)
                .changeState();

        // 2. Set variables for context
        taskService.setVariable(taskId, "sendBackReason", reason);
        taskService.setVariable(taskId, "sendBackFrom", currentActivityId);
        taskService.setVariable(taskId, "sendBackTo", targetActivityId);
        taskService.setVariable(taskId, "isSendBack", true);

        // 3. Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.valueOf("TASK_SENT_BACK")) // Needs enum update
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(UUID.fromString(userId))
                .actorName(userName)
                .metadata(Map.of(
                        "action", "sendBack",
                        "targetActivityId", targetActivityId,
                        "reason", reason))
                .build();
        actionTimelineRepository.save(timelineEvent);
    }

    /**
     * Get full audit history for a process instance.
     */
    @Transactional(readOnly = true)
    public List<ActionTimeline> getHistory(String processInstanceId) {
        return actionTimelineRepository.findByProcessInstanceIdOrderByCreatedAtDesc(processInstanceId);
    }

    // ==================== PARALLEL EXECUTION TRACKING ====================

    /**
     * Get parallel execution status for a process instance.
     * Supports deep nested parallel gateways with hierarchical tracking.
     */
    @Transactional(readOnly = true)
    public ParallelExecutionStatusDTO getParallelExecutionStatus(String processInstanceId) {
        log.debug("Getting parallel execution status for process: {}", processInstanceId);

        // Get all executions for this process
        List<org.flowable.engine.runtime.Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (executions.isEmpty()) {
            return ParallelExecutionStatusDTO.builder()
                    .processInstanceId(processInstanceId)
                    .isInParallelExecution(false)
                    .totalBranches(0)
                    .completedBranches(0)
                    .activeBranches(0)
                    .activeTasks(List.of())
                    .allActiveExecutions(List.of())
                    .maxNestingDepth(0)
                    .gateways(Map.of())
                    .build();
        }

        // Build execution hierarchy
        java.util.Map<String, ExecutionDTO> executionMap = new java.util.HashMap<>();
        java.util.Map<String, List<ExecutionDTO>> childrenMap = new java.util.HashMap<>();

        for (org.flowable.engine.runtime.Execution exec : executions) {
            // Flowable Execution interface - derive active from !isEnded() and activityId
            // presence
            boolean isActive = exec.getActivityId() != null && !exec.isEnded();
            // Scope: use parent check as proxy (root execution has no parent)
            boolean isScope = exec.getParentId() == null || exec.getId().equals(exec.getProcessInstanceId());

            ExecutionDTO dto = ExecutionDTO.builder()
                    .executionId(exec.getId())
                    .parentExecutionId(exec.getParentId())
                    .processInstanceId(exec.getProcessInstanceId())
                    .activityId(exec.getActivityId())
                    .activityName(getActivityName(processInstanceId, exec.getActivityId()))
                    .activityType(getActivityType(processInstanceId, exec.getActivityId()))
                    .active(isActive)
                    .ended(exec.isEnded())
                    .scope(isScope)
                    .nestingLevel(0) // Will calculate later
                    .childExecutions(new java.util.ArrayList<>())
                    .build();

            executionMap.put(exec.getId(), dto);

            if (exec.getParentId() != null) {
                childrenMap.computeIfAbsent(exec.getParentId(), k -> new java.util.ArrayList<>()).add(dto);
            }
        }

        // Build tree and calculate nesting levels
        ExecutionDTO rootExecution = null;
        for (ExecutionDTO dto : executionMap.values()) {
            if (dto.getParentExecutionId() == null || dto.getExecutionId().equals(processInstanceId)) {
                rootExecution = dto;
            }
            List<ExecutionDTO> children = childrenMap.get(dto.getExecutionId());
            if (children != null) {
                dto.setChildExecutions(children);
            }
        }

        // Calculate nesting levels
        int maxNestingDepth = calculateNestingLevels(rootExecution, 0);

        // Get active tasks
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        List<TaskDTO> activeTaskDTOs = activeTasks.stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());

        // Calculate branch counts
        List<ExecutionDTO> activeExecutions = executionMap.values().stream()
                .filter(e -> e.isActive() && e.getActivityId() != null)
                .collect(java.util.stream.Collectors.toList());

        // Detect parallel execution: more than 1 active execution with activities
        boolean isInParallel = activeExecutions.size() > 1 ||
                executions.stream().anyMatch(e -> getActivityType(processInstanceId, e.getActivityId()) != null &&
                        getActivityType(processInstanceId, e.getActivityId()).contains("parallelGateway"));

        // Get gateway completion info
        java.util.Map<String, ParallelExecutionStatusDTO.GatewayCompletionInfo> gateways = getGatewayCompletionInfo(
                processInstanceId, executions);

        return ParallelExecutionStatusDTO.builder().processInstanceId(processInstanceId)
                .isInParallelExecution(isInParallel).totalBranches(activeExecutions.size() +

                        getCompletedBranchCount(processInstanceId))
                .completedBranches(getCompletedBranchCount(processInstanceId))
                .activeBranches(activeExecutions.size())
                .activeTasks(activeTaskDTOs)
                .executionTree(rootExecution)
                .allActiveExecutions(activeExecutions)
                .maxNestingDepth(maxNestingDepth)
                .gateways(gateways)
                .build();
    }

    /**
     * Get all active executions for a process (flat list).
     */
    @Transactional(readOnly = true)
    public List<ExecutionDTO> getActiveExecutions(String processInstanceId) {
        List<org.flowable.engine.runtime.Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .onlyChildExecutions()
                .list();

        List<ExecutionDTO> result = new java.util.ArrayList<>();
        for (org.flowable.engine.runtime.Execution exec : executions) {
            boolean isActive = exec.getActivityId() != null && !exec.isEnded();
            boolean isScope = exec.getParentId() == null;

            result.add(ExecutionDTO.builder()
                    .executionId(exec.getId())
                    .parentExecutionId(exec.getParentId())
                    .processInstanceId(exec.getProcessInstanceId())
                    .activityId(exec.getActivityId())
                    .activityName(getActivityName(processInstanceId, exec.getActivityId()))
                    .activityType(getActivityType(processInstanceId, exec.getActivityId()))
                    .active(isActive)
                    .ended(exec.isEnded())
                    .scope(isScope)
                    .nestingLevel(calculateExecutionNestingLevel(exec.getId(), processInstanceId))
                    .build());
        }
        return result;
    }

    private int calculateNestingLevels(ExecutionDTO dto, int currentLevel) {
        if (dto == null)
            return currentLevel;
        dto.setNestingLevel(currentLevel);

        int maxChildLevel = currentLevel;
        if (dto.getChildExecutions() != null) {
            for (ExecutionDTO child : dto.getChildExecutions()) {
                int childLevel = calculateNestingLevels(child, currentLevel + 1);
                maxChildLevel = Math.max(maxChildLevel, childLevel);
            }
        }
        return maxChildLevel;
    }

    private int calculateExecutionNestingLevel(String executionId, String processInstanceId) {
        int level = 0;
        String currentId = executionId;
        while (currentId != null && !currentId.equals(processInstanceId)) {
            org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
                    .executionId(currentId)
                    .singleResult();
            if (exec == null)
                break;
            currentId = exec.getParentId();
            level++;
        }
        return level;
    }

    private String getActivityName(String processInstanceId, String activityId) {
        if (activityId == null)
            return null;
        try {
            org.flowable.engine.runtime.ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                org.flowable.bpmn.model.BpmnModel model = org.flowable.engine.ProcessEngines.getDefaultProcessEngine()
                        .getRepositoryService()
                        .getBpmnModel(pi.getProcessDefinitionId());
                org.flowable.bpmn.model.FlowElement element = model.getFlowElement(activityId);
                return element != null ? element.getName() : activityId;
            }
        } catch (Exception e) {
            log.trace("Could not get activity name: {}", e.getMessage());
        }
        return activityId;
    }

    private String getActivityType(String processInstanceId, String activityId) {
        if (activityId == null)
            return null;
        try {
            org.flowable.engine.runtime.ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                org.flowable.bpmn.model.BpmnModel model = org.flowable.engine.ProcessEngines.getDefaultProcessEngine()
                        .getRepositoryService()
                        .getBpmnModel(pi.getProcessDefinitionId());
                org.flowable.bpmn.model.FlowElement element = model.getFlowElement(activityId);
                return element != null ? element.getClass().getSimpleName() : null;
            }
        } catch (Exception e) {
            log.trace("Could not get activity type: {}", e.getMessage());
        }
        return null;
    }

    private int getCompletedBranchCount(String processInstanceId) {
        // Query historical completed activities that are join gateways
        // This is a simplified version; in production, you'd track this more explicitly
        return 0; // Will be enhanced with actual tracking
    }

    private java.util.Map<String, ParallelExecutionStatusDTO.GatewayCompletionInfo> getGatewayCompletionInfo(
            String processInstanceId, List<org.flowable.engine.runtime.Execution> executions) {
        java.util.Map<String, ParallelExecutionStatusDTO.GatewayCompletionInfo> result = new java.util.HashMap<>();

        // Find executions waiting at gateways
        for (org.flowable.engine.runtime.Execution exec : executions) {
            String activityType = getActivityType(processInstanceId, exec.getActivityId());
            if (activityType != null && activityType.contains("Gateway")) {
                String gatewayId = exec.getActivityId();
                if (!result.containsKey(gatewayId)) {
                    result.put(gatewayId, ParallelExecutionStatusDTO.GatewayCompletionInfo.builder()
                            .gatewayId(gatewayId)
                            .gatewayType(activityType)
                            .totalIncoming(getGatewayIncomingCount(processInstanceId, gatewayId))
                            .completedIncoming(countExecutionsAtGateway(executions, gatewayId))
                            .satisfied(false) // Will be calculated
                            .build());
                }
            }
        }

        return result;
    }

    private int getGatewayIncomingCount(String processInstanceId, String gatewayId) {
        try {
            org.flowable.engine.runtime.ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                org.flowable.bpmn.model.BpmnModel model = org.flowable.engine.ProcessEngines.getDefaultProcessEngine()
                        .getRepositoryService()
                        .getBpmnModel(pi.getProcessDefinitionId());
                org.flowable.bpmn.model.FlowElement element = model.getFlowElement(gatewayId);
                if (element instanceof org.flowable.bpmn.model.Gateway gateway) {
                    return gateway.getIncomingFlows().size();
                }
            }
        } catch (Exception e) {
            log.trace("Could not get gateway incoming count: {}", e.getMessage());
        }
        return 0;
    }

    private int countExecutionsAtGateway(List<org.flowable.engine.runtime.Execution> executions, String gatewayId) {
        return (int) executions.stream()
                .filter(e -> gatewayId.equals(e.getActivityId()))
                .count();
    }

    private TaskDTO toDTO(Task task) {
        // Try to get process metadata for additional context
        ProcessInstanceMetadata metadata = null;
        ProcessTemplate template = null;
        UUID formDefinitionId = null;

        try {
            metadata = processInstanceMetadataRepository
                    .findByFlowableProcessInstanceId(task.getProcessInstanceId())
                    .orElse(null);

            if (metadata != null && metadata.getProcessTemplate() != null) {
                template = metadata.getProcessTemplate();

                // Get form mapping for this task
                ProcessTemplateForm formMapping = processTemplateFormRepository
                        .findByProcessTemplateIdAndTaskKey(template.getId(), task.getTaskDefinitionKey())
                        .orElse(null);

                if (formMapping != null) {
                    formDefinitionId = formMapping.getFormDefinitionId();
                }
            }
        } catch (Exception e) {
            log.warn("Error loading process metadata for task {}: {}", task.getId(), e.getMessage());
        }

        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processTemplateId(template != null ? template.getId() : null)
                .processTemplateName(template != null ? template.getName() : null)
                .productId(metadata != null ? metadata.getProductId() : null)
                .businessKey(metadata != null ? metadata.getBusinessKey() : null)
                .processTitle(metadata != null ? metadata.getTitle() : null)
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .priority(task.getPriority())
                .createTime(task.getCreateTime() != null
                        ? task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .claimTime(task.getClaimTime() != null
                        ? task.getClaimTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .formDefinitionId(formDefinitionId)
                .build();
    }

    /**
     * Notify memo-service that a task has been completed.
     */
    private void notifyMemoServiceTaskCompleted(String taskId, String action, String completedBy) {
        try {
            Map<String, String> payload = Map.of(
                    "taskId", taskId,
                    "action", action,
                    "completedBy", completedBy != null ? completedBy : "");

            webClientBuilder.build()
                    .post()
                    .uri(memoServiceUrl + "/api/workflow-webhook/task-completed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Notified memo-service of task completion: {}", taskId);
        } catch (Exception e) {
            log.warn("Failed to notify memo-service of task completion: {}", e.getMessage());
        }
    }
}
