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

import com.cas.common.webclient.InternalWebClient;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @InternalWebClient
    private final WebClient.Builder webClientBuilder;
    @Lazy
    private final ProcessAnalyzerService processAnalyzerService;
    private final AuditLogger auditLogger;
    private final ActionValidationService actionValidationService;

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
     * Get the user's inbox with pagination, sorting, and filtering.
     * Combines assigned + candidate tasks, then applies in-memory
     * filtering, sorting, and pagination.
     *
     * @param userId   the user ID
     * @param groups   the user's role/group IDs
     * @param page     0-based page number
     * @param size     items per page
     * @param sortBy   field to sort by (createTime, priority, name)
     * @param sortDir  sort direction (asc, desc)
     * @param priority optional priority filter (HIGH, URGENT, NORMAL)
     * @param search   optional search term (matches task name or process title)
     * @return paginated task data
     */
    @Transactional(readOnly = true)
    public com.cas.common.dto.PagedData<TaskDTO> getInboxPaged(
            String userId, List<String> groups,
            int page, int size,
            String sortBy, String sortDir,
            String priority, String search) {

        // Combine assigned + candidate tasks
        List<TaskDTO> assigned = getAssignedTasks(userId);
        List<TaskDTO> candidate = getCandidateTasks(userId, groups);

        List<TaskDTO> all = new ArrayList<>(assigned);
        all.addAll(candidate);

        // De-duplicate by task ID (task could appear in both lists)
        all = all.stream()
                .collect(java.util.stream.Collectors.toMap(TaskDTO::getId, t -> t, (a, b) -> a))
                .values().stream()
                .collect(java.util.stream.Collectors.toList());

        // ── Filter ─────────────────────────────────────────────
        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) {
            int priorityValue = switch (priority.toUpperCase()) {
                case "URGENT" -> 100;
                case "HIGH" -> 75;
                case "NORMAL" -> 50;
                default -> -1;
            };
            if (priorityValue > 0) {
                all = all.stream()
                        .filter(t -> t.getPriority() != null && t.getPriority() == priorityValue)
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            all = all.stream()
                    .filter(t -> {
                        String name = t.getName() != null ? t.getName().toLowerCase() : "";
                        String title = t.getProcessTitle() != null ? t.getProcessTitle().toLowerCase() : "";
                        String template = t.getProcessTemplateName() != null
                                ? t.getProcessTemplateName().toLowerCase()
                                : "";
                        return name.contains(searchLower)
                                || title.contains(searchLower)
                                || template.contains(searchLower);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        long totalElements = all.size();

        // ── Sort ───────────────────────────────────────────────
        java.util.Comparator<TaskDTO> comparator = switch (sortBy != null ? sortBy : "createTime") {
            case "priority" -> java.util.Comparator.comparingInt(
                    t -> t.getPriority() != null ? t.getPriority() : 0);
            case "name" -> java.util.Comparator.comparing(
                    t -> t.getName() != null ? t.getName() : "", String.CASE_INSENSITIVE_ORDER);
            default -> java.util.Comparator.comparing(
                    TaskDTO::getCreateTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        };
        if ("desc".equalsIgnoreCase(sortDir) || sortDir == null) {
            comparator = comparator.reversed();
        }
        all.sort(comparator);

        // ── Paginate ───────────────────────────────────────────
        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<TaskDTO> pageContent = all.subList(fromIndex, toIndex);

        return com.cas.common.dto.PagedData.of(pageContent, page, size, totalElements, true);
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
     * 
     * ACTION-TYPE-AWARE ROUTING:
     * - APPROVE / ESCALATE / (unknown): Complete normally → follows BPMN path
     * - REJECT: Add comment, record audit, then TERMINATE the process
     * - BACK_TO_INITIATOR: Move token back to the first user task in the process
     * - BACK_TO_STEP: Handled separately via sendBackTask() endpoint
     * 
     * This allows admins to only draw the happy path (approve flow) in BPMN.
     */
    public void completeTask(String taskId, CompleteTaskRequest request, UUID userId, String userName,
            List<String> userRoles) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskKey = task.getTaskDefinitionKey();
        String actionLabel = request.getAction();
        Map<String, Object> actionOptionMeta = null;
        String actionType = null; // The programmatic action type (APPROVE, REJECT, etc.)

        // --- Action Validation & Type Resolution ---
        if (actionLabel != null && !actionLabel.isBlank()) {
            UUID processTemplateId = resolveProcessTemplateId(processInstanceId);
            if (processTemplateId != null) {
                // Validate the submitted action is in configured options
                actionValidationService.validateAction(processTemplateId, taskKey, actionLabel);
                // Validate comment requirement
                actionValidationService.validateComment(
                        processTemplateId, taskKey, actionLabel, request.getComment());
                // Get the full option metadata (includes actionType)
                actionOptionMeta = actionValidationService.getActionOption(
                        processTemplateId, taskKey, actionLabel);
                if (actionOptionMeta != null) {
                    actionType = (String) actionOptionMeta.get("actionType");
                }
            }
        }

        // Add comment if provided
        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskService.addComment(taskId, processInstanceId, request.getComment());
        }

        // --- Action-Type-Aware Routing ---
        if ("REJECT".equalsIgnoreCase(actionType)) {
            handleRejectAction(task, processInstanceId, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        } else if ("BACK_TO_INITIATOR".equalsIgnoreCase(actionType)) {
            handleBackToInitiatorAction(task, processInstanceId, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        } else if ("SEND_BACK".equalsIgnoreCase(actionType)) {
            handleSendBackAction(task, processInstanceId, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        } else if ("BACK_TO_STEP".equalsIgnoreCase(actionType)) {
            handleBackToStepAction(task, processInstanceId, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        } else if ("DELEGATE".equalsIgnoreCase(actionType)) {
            handleDelegateAction(task, processInstanceId, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        } else {
            // APPROVE, ESCALATE, or any unknown type → normal BPMN flow
            handleApproveAction(task, processInstanceId, taskKey, request, userId, userName, userRoles,
                    actionLabel, actionOptionMeta);
        }
    }

    /**
     * APPROVE / ESCALATE: Complete the task normally, following the drawn BPMN
     * path.
     */
    private void handleApproveAction(Task task, String processInstanceId, String taskKey,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();

        // Complete the task with variables → Flowable follows the BPMN sequence flow
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            taskService.complete(taskId, request.getVariables());
        } else {
            taskService.complete(taskId);
        }

        recordCompletionAudit(task, processInstanceId, taskKey, userId, userName, userRoles,
                actionLabel, actionOptionMeta, ActionTimeline.ActionType.TASK_COMPLETED, request.getComment());

        notifyMemoServiceTaskCompleted(taskId, actionLabel != null ? actionLabel : "APPROVE", userName);
        log.info("Task {} completed (APPROVE path) by user {}. Action: {}", taskId, userName, actionLabel);
    }

    /**
     * REJECT: Terminate the entire process instance.
     * The admin does NOT need to draw a reject path in BPMN.
     */
    private void handleRejectAction(Task task, String processInstanceId,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();
        String taskKey = task.getTaskDefinitionKey();
        String reason = request.getComment() != null ? request.getComment() : "Rejected by " + userName;

        // Capture active sibling tasks BEFORE termination (they'll be destroyed)
        List<Task> activeSiblings = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list()
                .stream()
                .filter(t -> !t.getId().equals(taskId))
                .toList();

        // Record audit BEFORE terminating (task still exists)
        recordCompletionAudit(task, processInstanceId, taskKey, userId, userName, userRoles,
                actionLabel, actionOptionMeta, ActionTimeline.ActionType.TASK_COMPLETED, request.getComment());

        // Terminate the process instance — removes all active tasks
        runtimeService.deleteProcessInstance(processInstanceId, "REJECTED: " + reason);

        // Record TASK_CANCELLED for each sibling that was destroyed by termination
        for (Task sibling : activeSiblings) {
            ActionTimeline cancelEvent = ActionTimeline.builder()
                    .processInstanceId(processInstanceId)
                    .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                    .taskId(sibling.getId())
                    .taskName(sibling.getName())
                    .actorId(userId)
                    .actorName(userName)
                    .metadata(Map.of(
                            "reason", "Cancelled due to rejection of " + task.getName(),
                            "triggeredBy", taskKey,
                            "cancelledByReject", "true"))
                    .build();
            actionTimelineRepository.save(cancelEvent);
            log.info("Recorded TASK_CANCELLED for sibling {} due to rejection", sibling.getName());
        }

        // Record PROCESS_CANCELLED event
        ActionTimeline processCancelledEvent = ActionTimeline.builder()
                .processInstanceId(processInstanceId)
                .actionType(ActionTimeline.ActionType.PROCESS_CANCELLED)
                .actorId(userId)
                .actorName(userName)
                .metadata(Map.of(
                        "reason", reason,
                        "rejectedBy", userName,
                        "rejectedTask", task.getName() != null ? task.getName() : taskKey))
                .build();
        actionTimelineRepository.save(processCancelledEvent);

        notifyMemoServiceTaskCompleted(taskId, "REJECTED", userName);
        log.info("Task {} REJECTED by user {}. Process {} terminated.", taskId, userName, processInstanceId);
    }

    /**
     * BACK_TO_INITIATOR: Move the token back to the very first user task in the
     * process.
     * Uses Flowable's changeActivityState API.
     */
    private void handleBackToInitiatorAction(Task task, String processInstanceId,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();
        String taskKey = task.getTaskDefinitionKey();

        // Find the first user task in the process history
        var historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();

        if (historicTasks.isEmpty()) {
            throw new IllegalStateException("No historic tasks found for process " + processInstanceId);
        }

        String firstTaskKey = historicTasks.get(0).getTaskDefinitionKey();
        log.info("BACK_TO_INITIATOR: Moving from {} to first task {}", taskKey, firstTaskKey);

        // Record audit BEFORE moving (task still exists at current position)
        recordCompletionAudit(task, processInstanceId, taskKey, userId, userName, userRoles,
                actionLabel, actionOptionMeta, ActionTimeline.ActionType.valueOf("TASK_SENT_BACK"),
                request.getComment());

        // Set context variables
        runtimeService.setVariable(processInstanceId, "returnedToInitiator", true);
        runtimeService.setVariable(processInstanceId, "returnReason",
                request.getComment() != null ? request.getComment() : "Returned by " + userName);
        runtimeService.setVariable(processInstanceId, "returnedBy", userName);

        // Move token back to the first user task
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(taskKey, firstTaskKey)
                .changeState();

        notifyMemoServiceTaskCompleted(taskId, "RETURNED_TO_INITIATOR", userName);
        log.info("Task {} sent back to initiator (first task: {}) by user {}",
                taskId, firstTaskKey, userName);
    }

    /**
     * SEND_BACK: Move the token back to the correct previous step(s).
     *
     * Gateway-aware logic:
     * - After a join gateway → moves to ALL leaf user tasks in the fork-join scope
     * (only those that actually executed)
     * - Inside a gateway branch → moves to the user task before the enclosing fork
     * - Otherwise → moves to the most recently completed task (naive fallback)
     *
     * Also auto-assigns the previous user/claimer on the newly created task(s).
     */
    private void handleSendBackAction(Task task, String processInstanceId,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();
        String taskKey = task.getTaskDefinitionKey();
        String processDefinitionId = task.getProcessDefinitionId();

        // Get completed task events from ActionTimeline
        List<ActionTimeline> completedTaskEvents = actionTimelineRepository
                .findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(
                        processInstanceId, ActionTimeline.ActionType.TASK_COMPLETED);

        // Resolve gateway-aware targets
        var sendBackTarget = processAnalyzerService.resolveSendBackTargets(
                processDefinitionId, taskKey, processInstanceId, completedTaskEvents);

        if (sendBackTarget.targetTaskKeys().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot send back: no previous step found for task " + taskKey);
        }

        log.info("SEND_BACK: Moving from {} to targets {} (multiTarget={})",
                taskKey, sendBackTarget.targetTaskKeys(), sendBackTarget.multiTarget());

        // Record audit BEFORE moving — include sentBackTo in metadata
        Map<String, Object> extraMeta = new java.util.HashMap<>();
        if (actionOptionMeta != null)
            extraMeta.putAll(actionOptionMeta);
        extraMeta.put("sentBackFrom", taskKey);
        extraMeta.put("sentBackTo", sendBackTarget.targetTaskKeys());
        recordCompletionAudit(task, processInstanceId, taskKey, userId, userName, userRoles,
                actionLabel, extraMeta, ActionTimeline.ActionType.TASK_SENT_BACK, request.getComment());

        // Set context variables
        runtimeService.setVariable(processInstanceId, "sentBack", true);
        runtimeService.setVariable(processInstanceId, "sentBackFrom", taskKey);
        runtimeService.setVariable(processInstanceId, "sentBackReason",
                request.getComment() != null ? request.getComment() : "Sent back by " + userName);
        runtimeService.setVariable(processInstanceId, "sentBackBy", userName);

        // Move token(s)
        if (sendBackTarget.multiTarget()) {
            // Multi-target: move to multiple activities (after join gateway)
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveSingleActivityIdToActivityIds(taskKey, sendBackTarget.targetTaskKeys())
                    .changeState();
        } else {
            String targetKey = sendBackTarget.targetTaskKeys().get(0);

            // Smart cancellation: walk through enclosing gateways layer by layer.
            // Only cancel active siblings when the target is OUTSIDE a gateway scope
            // (i.e., we're crossing backwards past a fork).
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            List<String> activityIdsToMove = new ArrayList<>();
            activityIdsToMove.add(taskKey); // always include the current task
            List<String> siblingTaskIdsToNotify = new ArrayList<>();
            List<Task> siblingTasks = new ArrayList<>();

            // Walk through enclosing gateways from innermost to outermost
            String currentElementKey = taskKey;
            Set<String> alreadyCancelledKeys = new HashSet<>();
            alreadyCancelledKeys.add(taskKey); // don't cancel yourself

            while (true) {
                String enclosingForkId = processAnalyzerService
                        .findEnclosingParallelGateway(processDefinitionId, currentElementKey);

                if (enclosingForkId == null)
                    break;

                Set<String> gatewayTasks = processAnalyzerService
                        .getTasksInGatewayScope(processDefinitionId, enclosingForkId);

                if (gatewayTasks.contains(targetKey)) {
                    // Target is INSIDE this gateway scope → no crossing → no cancellation
                    log.info("SEND_BACK: Target {} is inside gateway {} scope — no sibling cancellation",
                            targetKey, enclosingForkId);
                    break;
                } else {
                    // Target is OUTSIDE this gateway scope → crossing the fork
                    // → cancel all active tasks in this scope (except current task and already
                    // cancelled)
                    log.info("SEND_BACK: Target {} is outside gateway {} scope — cancelling active descendants",
                            targetKey, enclosingForkId);

                    for (Task activeTask : activeTasks) {
                        String activeKey = activeTask.getTaskDefinitionKey();
                        if (!alreadyCancelledKeys.contains(activeKey) && gatewayTasks.contains(activeKey)) {
                            activityIdsToMove.add(activeKey);
                            siblingTaskIdsToNotify.add(activeTask.getId());
                            siblingTasks.add(activeTask);
                            alreadyCancelledKeys.add(activeKey);
                            log.info("SEND_BACK: Cancelling task {} ({}) — inside crossed gateway {}",
                                    activeTask.getId(), activeKey, enclosingForkId);
                        }
                    }

                    // Move up to check outer gateways (for nested gateway cases)
                    currentElementKey = enclosingForkId;
                }
            }

            // Execute the move
            if (activityIdsToMove.size() > 1) {
                // Multi-activity move (current task + cancelled siblings)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(processInstanceId)
                        .moveActivityIdsToSingleActivityId(activityIdsToMove, targetKey)
                        .changeState();
            } else {
                // Simple single move (no siblings to cancel)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(processInstanceId)
                        .moveActivityIdTo(taskKey, targetKey)
                        .changeState();
            }

            // Notify memo-service about cancelled sibling tasks
            for (String siblingId : siblingTaskIdsToNotify) {
                notifyMemoServiceTaskCompleted(siblingId, "CANCELLED", userName);
            }

            // Record TASK_CANCELLED in ActionTimeline for each cancelled sibling
            for (Task siblingTask : siblingTasks) {
                ActionTimeline cancelEvent = ActionTimeline.builder()
                        .processInstanceId(processInstanceId)
                        .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                        .taskId(siblingTask.getId())
                        .taskName(siblingTask.getName())
                        .actorId(userId)
                        .actorName(userName)
                        .metadata(Map.of(
                                "reason", "Cancelled due to send-back of " + task.getName(),
                                "triggeredBy", taskKey,
                                "cancelledBySendBack", "true"))
                        .build();
                actionTimelineRepository.save(cancelEvent);
                log.info("Recorded TASK_CANCELLED for sibling {} in timeline", siblingTask.getName());
            }
        }

        // Notify memo-service about newly created tasks (changeState doesn't fire
        // AssignmentTaskListener)
        notifyMemoServiceNewTasks(processInstanceId, sendBackTarget.targetTaskKeys());

        // Auto-assign previous users on newly created tasks
        autoAssignPreviousUsers(processInstanceId, sendBackTarget);

        notifyMemoServiceTaskCompleted(taskId, "SENT_BACK", userName);
        log.info("Task {} sent back to {} by user {}", taskId, sendBackTarget.targetTaskKeys(), userName);
    }

    /**
     * BACK_TO_STEP: Move the token to a specific step chosen by the user.
     * The target step comes from the variables.targetStep variable,
     * set by the frontend step-picker dialog.
     *
     * Enhanced: stores sentBackFrom/sentBackTo in audit, auto-assigns previous
     * user.
     */
    private void handleBackToStepAction(Task task, String processInstanceId,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();
        String taskKey = task.getTaskDefinitionKey();

        // Get target step from variables
        String targetStep = null;
        if (request.getVariables() != null) {
            targetStep = (String) request.getVariables().get("targetStep");
        }

        if (targetStep == null || targetStep.isBlank()) {
            throw new IllegalArgumentException(
                    "BACK_TO_STEP action requires 'targetStep' variable (target task definition key)");
        }

        log.info("BACK_TO_STEP: Moving from {} to user-selected step {}", taskKey, targetStep);

        // Record audit BEFORE moving — include jump tracking metadata
        Map<String, Object> extraMeta = new java.util.HashMap<>();
        if (actionOptionMeta != null)
            extraMeta.putAll(actionOptionMeta);
        extraMeta.put("sentBackFrom", taskKey);
        extraMeta.put("sentBackTo", List.of(targetStep));
        recordCompletionAudit(task, processInstanceId, taskKey, userId, userName, userRoles,
                actionLabel, extraMeta, ActionTimeline.ActionType.TASK_SENT_BACK, request.getComment());

        // Set context variables
        runtimeService.setVariable(processInstanceId, "sentBack", true);
        runtimeService.setVariable(processInstanceId, "sentBackFrom", taskKey);
        runtimeService.setVariable(processInstanceId, "sentBackTo", targetStep);
        runtimeService.setVariable(processInstanceId, "sentBackReason",
                request.getComment() != null ? request.getComment() : "Sent back by " + userName);
        runtimeService.setVariable(processInstanceId, "sentBackBy", userName);

        // Move token to the selected step
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(taskKey, targetStep)
                .changeState();

        // Notify memo-service about newly created tasks
        notifyMemoServiceNewTasks(processInstanceId, List.of(targetStep));

        // Auto-assign previous user — look up from ActionTimeline
        List<ActionTimeline> completedTaskEvents = actionTimelineRepository
                .findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(
                        processInstanceId, ActionTimeline.ActionType.TASK_COMPLETED);
        Map<String, String> actorIds = new java.util.HashMap<>();
        Map<String, String> actorNames = new java.util.HashMap<>();
        for (int i = completedTaskEvents.size() - 1; i >= 0; i--) {
            var event = completedTaskEvents.get(i);
            Map<String, Object> meta = event.getMetadata();
            if (meta == null)
                continue;
            String tKey = (String) meta.get("taskDefinitionKey");
            if (targetStep.equals(tKey) && !actorIds.containsKey(tKey)) {
                if (event.getActorId() != null)
                    actorIds.put(tKey, event.getActorId().toString());
                if (event.getActorName() != null)
                    actorNames.put(tKey, event.getActorName());
            }
        }
        var target = new ProcessAnalyzerService.SendBackTarget(
                List.of(targetStep), false, actorIds, actorNames);
        autoAssignPreviousUsers(processInstanceId, target);

        notifyMemoServiceTaskCompleted(taskId, "SENT_BACK", userName);
        log.info("Task {} sent back to step {} by user {}", taskId, targetStep, userName);
    }

    /**
     * Auto-assign the previous user/claimer on newly created tasks after a
     * send-back.
     * Looks up active tasks in the process and matches them against the
     * SendBackTarget's
     * previous actor info.
     */
    private void autoAssignPreviousUsers(String processInstanceId,
            ProcessAnalyzerService.SendBackTarget sendBackTarget) {
        try {
            // Query active tasks in the process instance
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            for (Task activeTask : activeTasks) {
                String taskDefKey = activeTask.getTaskDefinitionKey();
                String previousActorId = sendBackTarget.previousActorIds().get(taskDefKey);
                String previousActorName = sendBackTarget.previousActorNames().get(taskDefKey);

                if (previousActorId != null && activeTask.getAssignee() == null) {
                    taskService.setAssignee(activeTask.getId(), previousActorId);
                    log.info("Auto-assigned task {} ({}) to previous user {} ({})",
                            activeTask.getId(), taskDefKey, previousActorName, previousActorId);
                }
            }
        } catch (Exception e) {
            // Auto-assignment is best-effort — don't fail the send-back
            log.warn("Failed to auto-assign previous users: {}", e.getMessage());
        }
    }

    /**
     * DELEGATE: Reassign the task to another user or group.
     * Does NOT change the workflow flow — the task stays at the same step.
     */
    private void handleDelegateAction(Task task, String processInstanceId,
            CompleteTaskRequest request, UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta) {

        String taskId = task.getId();
        String taskKey = task.getTaskDefinitionKey();

        // Get delegate target from variables
        String delegateTo = null;
        if (request.getVariables() != null) {
            delegateTo = (String) request.getVariables().get("delegateTo");
        }

        if (delegateTo == null || delegateTo.isBlank()) {
            throw new IllegalArgumentException("DELEGATE action requires 'delegateTo' variable (user ID or group ID)");
        }

        log.info("DELEGATE: Reassigning task {} from {} to {}", taskKey, userName, delegateTo);

        // Add comment if provided
        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskService.addComment(taskId, processInstanceId,
                    "Delegated by " + userName + ": " + request.getComment());
        }

        // Delegate the task using Flowable API
        taskService.delegateTask(taskId, delegateTo);

        // Record in timeline
        Map<String, Object> timelineMetadata = new java.util.HashMap<>();
        timelineMetadata.put("taskDefinitionKey", taskKey);
        timelineMetadata.put("action", actionLabel);
        timelineMetadata.put("actionType", "DELEGATE");
        timelineMetadata.put("delegatedTo", delegateTo);
        timelineMetadata.put("delegatedBy", userName);

        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(processInstanceId)
                .actionType(ActionTimeline.ActionType.TASK_DELEGATED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(userId)
                .actorName(userName)
                .actorRoles(userRoles)
                .metadata(timelineMetadata)
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Task {} delegated to {} by user {}", taskId, delegateTo, userName);
    }

    /**
     * Record completion in ActionTimeline and audit log.
     */
    private void recordCompletionAudit(Task task, String processInstanceId, String taskKey,
            UUID userId, String userName, List<String> userRoles,
            String actionLabel, Map<String, Object> actionOptionMeta,
            ActionTimeline.ActionType timelineActionType, String comment) {

        Map<String, Object> timelineMetadata = new java.util.HashMap<>();
        timelineMetadata.put("taskDefinitionKey", taskKey);
        if (actionLabel != null) {
            timelineMetadata.put("action", actionLabel);
        }
        if (comment != null && !comment.isBlank()) {
            timelineMetadata.put("comment", comment);
        }
        if (actionOptionMeta != null) {
            timelineMetadata.put("actionType", actionOptionMeta.get("actionType"));
            timelineMetadata.put("actionConfig", actionOptionMeta);
        }

        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(processInstanceId)
                .actionType(timelineActionType)
                .taskId(task.getId())
                .taskName(task.getName())
                .actorId(userId)
                .actorName(userName)
                .actorRoles(userRoles)
                .metadata(timelineMetadata)
                .build();
        actionTimelineRepository.save(timelineEvent);

        String actionForAudit = actionLabel != null ? actionLabel : "COMPLETE";
        auditLogger.log()
                .eventType(AuditEventType.COMPLETE)
                .action("Completed workflow task")
                .module("WORKFLOW")
                .entity("TASK", task.getId())
                .remarks("Task: " + task.getName() + ", Action: " + actionForAudit)
                .success();
    }

    /**
     * Resolve the processTemplateId from Flowable process variables.
     * Returns null if not found (backward compatibility with processes
     * started before processTemplateId was tracked).
     */
    private UUID resolveProcessTemplateId(String processInstanceId) {
        try {
            Object templateIdVar = runtimeService.getVariable(processInstanceId, "processTemplateId");
            if (templateIdVar == null) {
                log.debug("No processTemplateId variable for process {}", processInstanceId);
                return null;
            }
            return templateIdVar instanceof UUID
                    ? (UUID) templateIdVar
                    : UUID.fromString(templateIdVar.toString());
        } catch (Exception e) {
            log.warn("Failed to resolve processTemplateId for process {}: {}",
                    processInstanceId, e.getMessage());
            return null;
        }
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
    /**
     * Get delegate candidates for a task.
     * Returns candidate groups and candidate users from the Flowable task's
     * identity links.
     */
    public Map<String, Object> getDelegateCandidates(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Get identity links (candidate groups and users)
        var identityLinks = taskService.getIdentityLinksForTask(taskId);

        List<String> candidateGroups = identityLinks.stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(link -> link.getGroupId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        List<String> candidateUsers = identityLinks.stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                .map(link -> link.getUserId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("taskName", task.getName());
        result.put("candidateGroups", candidateGroups);
        result.put("candidateUsers", candidateUsers);
        result.put("currentAssignee", task.getAssignee());
        return result;
    }

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
        String processInstanceId = task.getProcessInstanceId();
        log.info("Sending back task {} from {} to {}", taskId, currentActivityId, targetActivityId);

        // 1. Record TASK_CANCELLED for sibling tasks that will be destroyed by
        // moveActivityIdTo
        // moveActivityIdTo cancels all active tasks in the process except the target
        List<Task> allActiveTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task siblingTask : allActiveTasks) {
            // Skip the task being sent back (it gets TASK_SENT_BACK, not TASK_CANCELLED)
            if (siblingTask.getId().equals(taskId))
                continue;

            log.info("Recording TASK_CANCELLED for sibling task {} ({}) due to send-back",
                    siblingTask.getId(), siblingTask.getName());
            ActionTimeline cancelEvent = ActionTimeline.builder()
                    .processInstanceId(processInstanceId)
                    .actionType(ActionTimeline.ActionType.TASK_CANCELLED)
                    .taskId(siblingTask.getId())
                    .taskName(siblingTask.getName())
                    .actorId(UUID.fromString(userId))
                    .actorName(userName)
                    .metadata(Map.of(
                            "reason", "Cancelled due to send-back of " + task.getName(),
                            "triggeredBy", currentActivityId,
                            "cancelledBySendBack", true))
                    .build();
            actionTimelineRepository.save(cancelEvent);
        }

        // 2. Move token using Change Activity State
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetActivityId)
                .changeState();

        // 3. Set variables for context
        taskService.setVariable(taskId, "sendBackReason", reason);
        taskService.setVariable(taskId, "sendBackFrom", currentActivityId);
        taskService.setVariable(taskId, "sendBackTo", targetActivityId);
        taskService.setVariable(taskId, "isSendBack", true);

        // 4. Record TASK_SENT_BACK in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(processInstanceId)
                .actionType(ActionTimeline.ActionType.valueOf("TASK_SENT_BACK"))
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
     * Includes retry logic for resilience.
     */
    private void notifyMemoServiceTaskCompleted(String taskId, String action, String completedBy) {
        Map<String, String> payload = Map.of(
                "taskId", taskId,
                "action", action,
                "completedBy", completedBy != null ? completedBy : "");

        log.info("Notifying memo-service of task completion: taskId={}, action={}, completedBy={}",
                taskId, action, completedBy);

        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                webClientBuilder.build()
                        .post()
                        .uri(memoServiceUrl + "/api/workflow-webhook/task-completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                log.info("Successfully notified memo-service of task completion: {} (attempt {})",
                        taskId, attempt + 1);
                return;
            } catch (Exception e) {
                log.warn("Failed to notify memo-service (attempt {}/{}): taskId={}, error={}",
                        attempt + 1, maxRetries + 1, taskId, e.getMessage(), e);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("FAILED all retries to notify memo-service of task completion: taskId={}", taskId);
    }

    /**
     * After a changeState() call (send-back), Flowable creates new tasks but
     * does NOT fire the AssignmentTaskListener. We manually notify memo-service
     * to create MemoTask records ONLY for the specified target tasks.
     *
     * @param processInstanceId the process instance
     * @param targetTaskKeys    the BPMN task definition keys that were targets of
     *                          the state change
     */
    private void notifyMemoServiceNewTasks(String processInstanceId, List<String> targetTaskKeys) {
        try {
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            // Get memoId from process variables
            Object memoIdObj = runtimeService.getVariable(processInstanceId, "memoId");
            if (memoIdObj == null) {
                log.warn("No memoId found in process variables, cannot notify memo-service of new tasks");
                return;
            }
            String memoId = memoIdObj.toString();

            // Only notify for tasks that match the target keys
            Set<String> targetSet = new java.util.HashSet<>(targetTaskKeys);

            for (Task activeTask : activeTasks) {
                if (!targetSet.contains(activeTask.getTaskDefinitionKey())) {
                    continue; // Skip tasks that aren't targets of this send-back
                }

                log.info("Notifying memo-service of new task: {} ({}) in process {}",
                        activeTask.getName(), activeTask.getTaskDefinitionKey(), processInstanceId);

                // Get candidates from Flowable identity links
                List<String> candidateGroups = new ArrayList<>();
                List<String> candidateUsers = new ArrayList<>();
                try {
                    var identityLinks = taskService.getIdentityLinksForTask(activeTask.getId());
                    for (var link : identityLinks) {
                        if ("candidate".equals(link.getType()) && link.getGroupId() != null) {
                            candidateGroups.add(link.getGroupId());
                        }
                        if ("candidate".equals(link.getType()) && link.getUserId() != null) {
                            candidateUsers.add(link.getUserId());
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not resolve identity links for task {}: {}", activeTask.getId(), e.getMessage());
                }

                // Send task-created webhook to memo-service
                Map<String, Object> event = new HashMap<>();
                event.put("taskId", activeTask.getId());
                event.put("memoId", memoId);
                event.put("taskDefinitionKey", activeTask.getTaskDefinitionKey());
                event.put("taskName", activeTask.getName());
                event.put("processInstanceId", processInstanceId);
                event.put("candidateGroups", candidateGroups);
                event.put("candidateUsers", candidateUsers);

                try {
                    webClientBuilder.build()
                            .post()
                            .uri(memoServiceUrl + "/api/workflow-webhook/task-created")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(event)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();
                    log.info("Successfully notified memo-service of new task: {} ({})",
                            activeTask.getId(), activeTask.getTaskDefinitionKey());
                } catch (Exception e) {
                    log.warn("Failed to notify memo-service of new task {}: {}",
                            activeTask.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error notifying memo-service of new tasks: {}", e.getMessage(), e);
        }
    }
}
