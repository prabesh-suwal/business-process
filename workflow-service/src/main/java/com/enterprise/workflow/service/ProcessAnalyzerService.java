package com.enterprise.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for analyzing BPMN process structure and execution paths.
 * Used for detecting parallel gateway scopes and sibling branch relationships.
 * 
 * Supports nested gateways by properly tracking execution hierarchy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessAnalyzerService {

    private final RepositoryService repositoryService;
    private final TaskService taskService;

    /**
     * Find the parallel gateway (fork) that a task belongs to.
     * This analyzes the BPMN model to find the enclosing parallel scope.
     * 
     * @param processDefinitionId The process definition ID
     * @param taskDefinitionKey   The task definition key
     * @return The gateway ID if found, null otherwise
     */
    public String findEnclosingParallelGateway(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        Process process = bpmnModel.getMainProcess();

        // Find the task element
        FlowElement taskElement = process.getFlowElement(taskDefinitionKey);
        if (taskElement == null) {
            log.warn("Task element not found: {}", taskDefinitionKey);
            return null;
        }

        // Traverse backwards to find the fork gateway
        return findForkGatewayForElement(process, taskElement, new HashSet<>());
    }

    /**
     * Recursively traverse backwards to find the gateway (fork) that leads
     * to this element. Recognizes Parallel, Inclusive, and Exclusive gateways.
     */
    private String findForkGatewayForElement(Process process, FlowElement element, Set<String> visited) {
        if (element == null || visited.contains(element.getId())) {
            return null;
        }
        visited.add(element.getId());

        // Get all incoming flows
        if (element instanceof FlowNode flowNode) {
            List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();

            for (SequenceFlow flow : incomingFlows) {
                FlowElement source = process.getFlowElement(flow.getSourceRef());

                // Check if source is a gateway with multiple outgoing flows (fork)
                if (source instanceof ParallelGateway || source instanceof InclusiveGateway
                        || source instanceof ExclusiveGateway) {
                    Gateway gateway = (Gateway) source;
                    if (gateway.getOutgoingFlows().size() > 1) {
                        log.debug("Found fork gateway: {} ({}) for element: {}",
                                gateway.getId(), gateway.getClass().getSimpleName(), element.getId());
                        return gateway.getId();
                    }
                }

                // Continue traversing backwards
                String result = findForkGatewayForElement(process, source, visited);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Get all task definition keys that are within a specific gateway's scope.
     * This includes tasks in all branches between the fork and corresponding join
     * gateway.
     * 
     * @param processDefinitionId The process definition ID
     * @param gatewayId           The fork gateway ID
     * @return Set of task definition keys within this gateway's scope
     */
    public Set<String> getTasksInGatewayScope(String processDefinitionId, String gatewayId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        Process process = bpmnModel.getMainProcess();

        FlowElement gatewayElement = process.getFlowElement(gatewayId);
        if (!(gatewayElement instanceof Gateway gateway)) {
            log.warn("Gateway not found or not a gateway: {}", gatewayId);
            return Collections.emptySet();
        }

        Set<String> tasksInScope = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Find the corresponding join gateway
        String joinGatewayId = findJoinGateway(process, gateway);

        // Traverse each outgoing branch and collect tasks until we hit the join
        for (SequenceFlow outgoingFlow : gateway.getOutgoingFlows()) {
            collectTasksInBranch(process, outgoingFlow.getTargetRef(), joinGatewayId, tasksInScope, visited);
        }

        log.debug("Tasks in scope of gateway {}: {}", gatewayId, tasksInScope);
        return tasksInScope;
    }

    /**
     * Find the corresponding join gateway for a fork gateway.
     * The join gateway is where all branches from the fork converge.
     */
    private String findJoinGateway(Process process, Gateway forkGateway) {
        // For each branch, trace forward until we find a gateway with multiple incoming
        // flows
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // Start with all targets of the fork
        for (SequenceFlow flow : forkGateway.getOutgoingFlows()) {
            queue.add(flow.getTargetRef());
        }

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (visited.contains(currentId))
                continue;
            visited.add(currentId);

            FlowElement current = process.getFlowElement(currentId);

            // Check if this is a gateway with multiple incoming flows (candidate for join)
            if (current instanceof ParallelGateway || current instanceof InclusiveGateway) {
                Gateway gateway = (Gateway) current;
                if (gateway.getIncomingFlows().size() > 1) {
                    log.debug("Found join gateway {} for fork {}", currentId, forkGateway.getId());
                    return currentId;
                }
            }

            // Continue forward
            if (current instanceof FlowNode flowNode) {
                for (SequenceFlow flow : flowNode.getOutgoingFlows()) {
                    queue.add(flow.getTargetRef());
                }
            }
        }

        return null;
    }

    /**
     * Collect all user tasks between a start element and a stop element (join
     * gateway).
     */
    private void collectTasksInBranch(Process process, String currentId, String joinGatewayId,
            Set<String> tasksInScope, Set<String> visited) {
        if (currentId == null || visited.contains(currentId)) {
            return;
        }

        // Stop if we've reached the join gateway
        if (currentId.equals(joinGatewayId)) {
            return;
        }

        visited.add(currentId);
        FlowElement element = process.getFlowElement(currentId);

        // If it's a user task, add it to the scope
        if (element instanceof UserTask) {
            tasksInScope.add(element.getId());
        }

        // If it's a nested gateway, still traverse through it (for nested support)

        // Continue forward
        if (element instanceof FlowNode flowNode) {
            for (SequenceFlow flow : flowNode.getOutgoingFlows()) {
                collectTasksInBranch(process, flow.getTargetRef(), joinGatewayId, tasksInScope, visited);
            }
        }
    }

    /**
     * Get all active tasks that belong to sibling branches of the current task.
     * This is the key method for ANY completion mode - it finds all tasks that
     * should
     * be cancelled when one branch completes.
     * 
     * SUPPORTS NESTED GATEWAYS: Only cancels tasks in the same parallel scope,
     * not tasks in parent or child gateway scopes.
     * 
     * @param processInstanceId The running process instance ID
     * @param currentTaskId     The task that is being completed
     * @param gatewayId         The parallel gateway ID that defines the scope
     * @return List of task IDs that should be cancelled
     */
    public List<String> getSiblingBranchTasksToCancel(String processInstanceId, String currentTaskId,
            String gatewayId) {
        Task currentTask = taskService.createTaskQuery()
                .taskId(currentTaskId)
                .singleResult();

        if (currentTask == null) {
            log.warn("Current task not found: {}", currentTaskId);
            return Collections.emptyList();
        }

        String processDefinitionId = currentTask.getProcessDefinitionId();
        String currentTaskDefKey = currentTask.getTaskDefinitionKey();

        // Get all tasks within this gateway's scope
        Set<String> tasksInGatewayScope = getTasksInGatewayScope(processDefinitionId, gatewayId);

        // Get all active tasks in the process instance
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();

        List<String> tasksToCancel = new ArrayList<>();

        for (Task activeTask : activeTasks) {
            // Skip the current task
            if (activeTask.getId().equals(currentTaskId)) {
                continue;
            }

            // Only cancel if the task is within the gateway scope
            if (tasksInGatewayScope.contains(activeTask.getTaskDefinitionKey())) {
                // Also check that it's NOT the same task def key (same task type in parallel)
                if (!activeTask.getTaskDefinitionKey().equals(currentTaskDefKey)) {
                    tasksToCancel.add(activeTask.getId());
                    log.debug("Task {} ({}) in gateway {} scope - will be cancelled",
                            activeTask.getId(), activeTask.getTaskDefinitionKey(), gatewayId);
                }
            } else {
                log.debug("Task {} ({}) NOT in gateway {} scope - will NOT be cancelled",
                        activeTask.getId(), activeTask.getTaskDefinitionKey(), gatewayId);
            }
        }

        return tasksToCancel;
    }

    /**
     * Check if a task belongs to a specific gateway's parallel scope.
     * 
     * @param processDefinitionId The process definition ID
     * @param taskDefinitionKey   The task definition key
     * @param gatewayId           The gateway ID to check scope for
     * @return true if the task is within the gateway's scope
     */
    public boolean isTaskInGatewayScope(String processDefinitionId, String taskDefinitionKey, String gatewayId) {
        Set<String> tasksInScope = getTasksInGatewayScope(processDefinitionId, gatewayId);
        return tasksInScope.contains(taskDefinitionKey);
    }

    /**
     * Get all parallel/inclusive gateways in a process that are configured as
     * forks.
     * 
     * @param processDefinitionId The process definition ID
     * @return Map of gateway ID to gateway type
     */
    public Map<String, String> getParallelForkGateways(String processDefinitionId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        Process process = bpmnModel.getMainProcess();

        Map<String, String> forkGateways = new HashMap<>();

        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof ParallelGateway gateway) {
                if (gateway.getOutgoingFlows().size() > 1) {
                    forkGateways.put(gateway.getId(), "parallelGateway");
                }
            } else if (element instanceof InclusiveGateway gateway) {
                if (gateway.getOutgoingFlows().size() > 1) {
                    forkGateways.put(gateway.getId(), "inclusiveGateway");
                }
            }
        }

        return forkGateways;
    }

    /**
     * Result of gateway-aware send-back target resolution.
     */
    public record SendBackTarget(
            List<String> targetTaskKeys,
            boolean multiTarget,
            /** Previous actor IDs for each target task key (from ActionTimeline) */
            Map<String, String> previousActorIds,
            Map<String, String> previousActorNames) {
    }

    /**
     * Resolve the correct SEND_BACK targets for a task, respecting gateway scopes.
     *
     * Rules:
     * 1. If the task's BPMN predecessor is a JOIN gateway → return ALL leaf user
     * tasks
     * inside the fork-join scope that actually executed (multi-target)
     * 2. If the task is INSIDE a gateway branch → return the user task before the
     * fork
     * 3. Otherwise → return the most recently completed task from history (naive)
     *
     * @param processDefinitionId the process definition
     * @param currentTaskKey      the current task's definition key
     * @param processInstanceId   the running process instance (for ActionTimeline
     *                            queries)
     * @param completedTaskEvents list of TASK_COMPLETED ActionTimeline events for
     *                            this process
     * @return SendBackTarget with the target task keys
     */
    public SendBackTarget resolveSendBackTargets(
            String processDefinitionId,
            String currentTaskKey,
            String processInstanceId,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents) {

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        Process process = bpmnModel.getMainProcess();

        FlowElement currentElement = process.getFlowElement(currentTaskKey);
        if (currentElement == null) {
            log.warn("Task element not found in BPMN model: {}", currentTaskKey);
            return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
        }

        // Step 1: Check if the current task's immediate predecessor is a JOIN gateway
        if (currentElement instanceof FlowNode flowNode) {
            for (SequenceFlow incoming : flowNode.getIncomingFlows()) {
                FlowElement predecessor = process.getFlowElement(incoming.getSourceRef());
                if (isJoinGateway(predecessor)) {
                    log.info("Task {} is after join gateway {} — resolving multi-target send-back",
                            currentTaskKey, predecessor.getId());
                    return resolveAfterJoinGateway(process, (Gateway) predecessor,
                            currentTaskKey, completedTaskEvents);
                }
            }
        }

        // Step 2: Check if the current task is INSIDE a gateway branch
        String enclosingForkId = findEnclosingParallelGateway(processDefinitionId, currentTaskKey);
        if (enclosingForkId != null) {
            log.info("Task {} is inside gateway branch (fork: {}) — resolving to task before fork",
                    currentTaskKey, enclosingForkId);
            return resolveInsideGatewayBranch(process, enclosingForkId, currentTaskKey, completedTaskEvents);
        }

        // Step 3: Fallback — naive history (most recent completed task with different
        // key)
        log.info("Task {} has no gateway context — using naive history fallback", currentTaskKey);
        return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
    }

    /**
     * Current task is AFTER a join gateway.
     * Find all leaf user tasks inside the fork-join scope that actually executed.
     */
    private SendBackTarget resolveAfterJoinGateway(
            Process process, Gateway joinGateway,
            String currentTaskKey,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents) {

        // Find the matching fork gateway
        Gateway forkGateway = findMatchingForkGateway(process, joinGateway);
        if (forkGateway == null) {
            log.warn("Could not find matching fork gateway for join: {}", joinGateway.getId());
            return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
        }

        // Get ALL leaf user tasks in the fork-join scope (recursively handles nested
        // gateways)
        Set<String> leafTasks = getLeafUserTasksInScope(process, forkGateway, joinGateway.getId());

        // Filter to only tasks that actually executed (important for inclusive
        // gateways)
        Set<String> executedTaskKeys = new HashSet<>();
        for (var event : completedTaskEvents) {
            Map<String, Object> meta = event.getMetadata();
            if (meta != null && meta.get("taskDefinitionKey") != null) {
                executedTaskKeys.add((String) meta.get("taskDefinitionKey"));
            }
        }

        List<String> targets = leafTasks.stream()
                .filter(executedTaskKeys::contains)
                .sorted()
                .toList();

        if (targets.isEmpty()) {
            log.warn("No executed leaf tasks found in gateway scope, falling back to naive history");
            return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
        }

        // Get previous actor info for each target
        Map<String, String> actorIds = new HashMap<>();
        Map<String, String> actorNames = new HashMap<>();
        populatePreviousActors(targets, completedTaskEvents, actorIds, actorNames);

        log.info("After-join send-back from {}: targets = {}", currentTaskKey, targets);
        return new SendBackTarget(targets, targets.size() > 1, actorIds, actorNames);
    }

    /**
     * Current task is INSIDE a gateway branch.
     * Return the user task BEFORE the enclosing fork gateway.
     */
    private SendBackTarget resolveInsideGatewayBranch(
            Process process, String forkGatewayId,
            String currentTaskKey,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents) {

        FlowElement forkElement = process.getFlowElement(forkGatewayId);
        if (!(forkElement instanceof Gateway forkGateway)) {
            return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
        }

        // Find the user task before the fork
        String taskBeforeFork = findUserTaskBeforeElement(process, forkGateway, new HashSet<>());
        if (taskBeforeFork == null) {
            log.warn("No user task found before fork gateway {}", forkGatewayId);
            return fallbackToNaiveHistory(currentTaskKey, completedTaskEvents);
        }

        Map<String, String> actorIds = new HashMap<>();
        Map<String, String> actorNames = new HashMap<>();
        populatePreviousActors(List.of(taskBeforeFork), completedTaskEvents, actorIds, actorNames);

        log.info("Inside-branch send-back from {}: target = {}", currentTaskKey, taskBeforeFork);
        return new SendBackTarget(List.of(taskBeforeFork), false, actorIds, actorNames);
    }

    /**
     * Fallback: use naive history to find the most recently completed task with a
     * different key.
     */
    private SendBackTarget fallbackToNaiveHistory(
            String currentTaskKey,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents) {

        // Events are ASC order, iterate from the end
        for (int i = completedTaskEvents.size() - 1; i >= 0; i--) {
            var event = completedTaskEvents.get(i);
            Map<String, Object> meta = event.getMetadata();
            if (meta == null)
                continue;
            String taskKey = (String) meta.get("taskDefinitionKey");
            if (taskKey != null && !taskKey.equals(currentTaskKey)) {
                Map<String, String> actorIds = new HashMap<>();
                Map<String, String> actorNames = new HashMap<>();
                populatePreviousActors(List.of(taskKey), completedTaskEvents, actorIds, actorNames);
                return new SendBackTarget(List.of(taskKey), false, actorIds, actorNames);
            }
        }

        return new SendBackTarget(List.of(), false, Map.of(), Map.of());
    }

    /**
     * Get the leaf user tasks within a fork-join scope.
     * "Leaf" means the task directly feeds the join (or feeds a nested join that
     * feeds the outer join).
     * Tasks that feed into a nested fork are NOT leaf tasks — instead, the nested
     * fork's leaf tasks are included.
     *
     * Example: A → Fork1 → B1 → Fork2 → C1,C2 → Join2 → Join1
     * Leaf tasks of Fork1-Join1: C1, C2 (NOT B1, because B1 feeds into Fork2)
     * If there's also B2 in a separate branch: C1, C2, B2
     */
    private Set<String> getLeafUserTasksInScope(Process process, Gateway forkGateway, String joinGatewayId) {
        Set<String> leafTasks = new HashSet<>();

        for (SequenceFlow outgoing : forkGateway.getOutgoingFlows()) {
            collectLeafUserTasks(process, outgoing.getTargetRef(), joinGatewayId, leafTasks, new HashSet<>());
        }

        return leafTasks;
    }

    /**
     * Recursively collect leaf user tasks in a branch until the join gateway.
     * A user task is a "leaf" if it doesn't lead to a nested fork before reaching
     * the join.
     */
    private void collectLeafUserTasks(Process process, String currentId, String joinGatewayId,
            Set<String> leafTasks, Set<String> visited) {
        if (currentId == null || visited.contains(currentId) || currentId.equals(joinGatewayId)) {
            return;
        }
        visited.add(currentId);

        FlowElement element = process.getFlowElement(currentId);

        if (element instanceof UserTask) {
            // Check if this user task leads to a nested fork gateway
            FlowNode taskNode = (FlowNode) element;
            boolean leadsToNestedFork = false;

            for (SequenceFlow outgoing : taskNode.getOutgoingFlows()) {
                FlowElement target = process.getFlowElement(outgoing.getTargetRef());
                if (isForkGateway(target)) {
                    // This user task feeds a nested fork — it's NOT a leaf task
                    // Instead, recursively collect leaf tasks from the nested fork
                    leadsToNestedFork = true;
                    Gateway nestedFork = (Gateway) target;
                    String nestedJoin = findJoinGateway(process, nestedFork);

                    // Collect leaf tasks from the nested fork scope
                    Set<String> nestedLeafs = getLeafUserTasksInScope(process, nestedFork,
                            nestedJoin != null ? nestedJoin : joinGatewayId);
                    leafTasks.addAll(nestedLeafs);

                    // Continue after the nested join
                    if (nestedJoin != null) {
                        FlowElement nestedJoinElement = process.getFlowElement(nestedJoin);
                        if (nestedJoinElement instanceof FlowNode nestedJoinNode) {
                            for (SequenceFlow afterJoin : nestedJoinNode.getOutgoingFlows()) {
                                collectLeafUserTasks(process, afterJoin.getTargetRef(),
                                        joinGatewayId, leafTasks, visited);
                            }
                        }
                    }
                }
            }

            if (!leadsToNestedFork) {
                // This is a real leaf user task
                leafTasks.add(element.getId());
            }
        } else if (isForkGateway(element)) {
            // Gateway directly after another gateway (no user task in between)
            Gateway nestedFork = (Gateway) element;
            String nestedJoin = findJoinGateway(process, nestedFork);
            Set<String> nestedLeafs = getLeafUserTasksInScope(process, nestedFork,
                    nestedJoin != null ? nestedJoin : joinGatewayId);
            leafTasks.addAll(nestedLeafs);

            if (nestedJoin != null) {
                FlowElement nestedJoinElement = process.getFlowElement(nestedJoin);
                if (nestedJoinElement instanceof FlowNode nestedJoinNode) {
                    for (SequenceFlow afterJoin : nestedJoinNode.getOutgoingFlows()) {
                        collectLeafUserTasks(process, afterJoin.getTargetRef(),
                                joinGatewayId, leafTasks, visited);
                    }
                }
            }
        } else if (element instanceof FlowNode flowNode) {
            // Service task or other — continue traversing
            for (SequenceFlow outgoing : flowNode.getOutgoingFlows()) {
                collectLeafUserTasks(process, outgoing.getTargetRef(), joinGatewayId, leafTasks, visited);
            }
        }
    }

    /**
     * Find the user task immediately before a given element by walking backwards.
     */
    private String findUserTaskBeforeElement(Process process, FlowElement element, Set<String> visited) {
        if (element == null || visited.contains(element.getId())) {
            return null;
        }
        visited.add(element.getId());

        if (element instanceof FlowNode flowNode) {
            for (SequenceFlow incoming : flowNode.getIncomingFlows()) {
                FlowElement source = process.getFlowElement(incoming.getSourceRef());
                if (source instanceof UserTask) {
                    return source.getId();
                }
                // If the source is a join gateway, go further back to find the task before it
                // (handles case of cascaded gateways)
                String result = findUserTaskBeforeElement(process, source, visited);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Find the matching fork gateway for a join gateway.
     * Walks backwards from the join to find a fork of the same gateway type.
     */
    private Gateway findMatchingForkGateway(Process process, Gateway joinGateway) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        for (SequenceFlow incoming : joinGateway.getIncomingFlows()) {
            queue.add(incoming.getSourceRef());
        }

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (visited.contains(currentId))
                continue;
            visited.add(currentId);

            FlowElement current = process.getFlowElement(currentId);

            if (isForkGateway(current)) {
                // Check if this fork's join is our join gateway
                String thisJoin = findJoinGateway(process, (Gateway) current);
                if (joinGateway.getId().equals(thisJoin)) {
                    return (Gateway) current;
                }
            }

            // Continue backwards
            if (current instanceof FlowNode flowNode) {
                for (SequenceFlow incoming : flowNode.getIncomingFlows()) {
                    queue.add(incoming.getSourceRef());
                }
            }
        }
        return null;
    }

    /**
     * Check if a flow element is a join gateway (multiple incoming flows).
     */
    private boolean isJoinGateway(FlowElement element) {
        if (element instanceof ParallelGateway gw) {
            return gw.getIncomingFlows().size() > 1;
        }
        if (element instanceof InclusiveGateway gw) {
            return gw.getIncomingFlows().size() > 1;
        }
        return false;
    }

    /**
     * Check if a flow element is a fork gateway (multiple outgoing flows).
     */
    private boolean isForkGateway(FlowElement element) {
        if (element instanceof ParallelGateway gw) {
            return gw.getOutgoingFlows().size() > 1;
        }
        if (element instanceof InclusiveGateway gw) {
            return gw.getOutgoingFlows().size() > 1;
        }
        return false;
    }

    /**
     * Populate previous actor info from ActionTimeline for auto-assignment.
     * For each target task key, find the most recent TASK_COMPLETED event.
     */
    private void populatePreviousActors(
            List<String> targetTaskKeys,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents,
            Map<String, String> actorIds,
            Map<String, String> actorNames) {

        // Iterate from latest to earliest
        Set<String> remaining = new HashSet<>(targetTaskKeys);
        for (int i = completedTaskEvents.size() - 1; i >= 0 && !remaining.isEmpty(); i--) {
            var event = completedTaskEvents.get(i);
            Map<String, Object> meta = event.getMetadata();
            if (meta == null)
                continue;
            String taskKey = (String) meta.get("taskDefinitionKey");
            if (taskKey != null && remaining.contains(taskKey)) {
                remaining.remove(taskKey);
                if (event.getActorId() != null) {
                    actorIds.put(taskKey, event.getActorId().toString());
                }
                if (event.getActorName() != null) {
                    actorNames.put(taskKey, event.getActorName());
                }
            }
        }
    }

    /**
     * Get valid return points for BACK_TO_STEP, filtered by gateway scope.
     *
     * If the current task is inside a gateway branch:
     * - Exclude sibling branch tasks (only show tasks before the fork)
     * If the current task is after a join:
     * - Show all tasks (user can pick individual steps)
     */
    public List<String> getValidReturnPointTaskKeys(
            String processDefinitionId, String currentTaskKey,
            List<com.enterprise.workflow.entity.ActionTimeline> completedTaskEvents) {

        // Get all completed task keys
        Set<String> allCompletedKeys = new LinkedHashSet<>();
        for (var event : completedTaskEvents) {
            Map<String, Object> meta = event.getMetadata();
            if (meta != null && meta.get("taskDefinitionKey") != null) {
                String key = (String) meta.get("taskDefinitionKey");
                if (!key.equals(currentTaskKey)) {
                    allCompletedKeys.add(key);
                }
            }
        }

        // Check if the current task is inside a gateway branch
        String enclosingForkId = findEnclosingParallelGateway(processDefinitionId, currentTaskKey);
        if (enclosingForkId != null) {
            // Get sibling tasks in this gateway scope
            Set<String> siblingTasks = getTasksInGatewayScope(processDefinitionId, enclosingForkId);
            // Remove sibling tasks from return points (except the current task itself,
            // already excluded)
            allCompletedKeys.removeAll(siblingTasks);
            log.debug("Filtered return points for {} (inside gateway {}): removed siblings {}",
                    currentTaskKey, enclosingForkId, siblingTasks);
        }

        return new ArrayList<>(allCompletedKeys);
    }
}
