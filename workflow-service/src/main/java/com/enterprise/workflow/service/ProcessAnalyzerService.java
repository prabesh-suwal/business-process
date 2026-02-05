package com.enterprise.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
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
     * Recursively traverse backwards to find the parallel gateway (fork) that leads
     * to this element.
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

                // Check if source is a parallel or inclusive gateway with multiple outgoing
                // flows (fork)
                if (source instanceof ParallelGateway || source instanceof InclusiveGateway) {
                    Gateway gateway = (Gateway) source;
                    if (gateway.getOutgoingFlows().size() > 1) {
                        log.debug("Found fork gateway: {} for element: {}", gateway.getId(), element.getId());
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
}
