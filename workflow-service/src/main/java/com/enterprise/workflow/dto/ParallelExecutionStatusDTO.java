package com.enterprise.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO representing the parallel execution status of a process instance.
 * Provides comprehensive tracking for parallel workflows including deep
 * nesting.
 */
@Data
@Builder
public class ParallelExecutionStatusDTO {

    /**
     * Process instance ID
     */
    private String processInstanceId;

    /**
     * Whether the process currently has parallel executions
     */
    private boolean isInParallelExecution;

    /**
     * Total number of parallel branches at the current level
     * For simple fork: 2-N branches
     * For deep nested: counts active branches at deepest active level
     */
    private int totalBranches;

    /**
     * Number of branches that have completed (reached join or end)
     */
    private int completedBranches;

    /**
     * Number of branches still active
     */
    private int activeBranches;

    /**
     * Currently active tasks across all parallel branches
     */
    private List<TaskDTO> activeTasks;

    /**
     * Hierarchical view of all executions (for UI visualization)
     * Root execution with nested child executions
     */
    private ExecutionDTO executionTree;

    /**
     * Flat list of all active executions (alternative to tree for simpler UIs)
     */
    private List<ExecutionDTO> allActiveExecutions;

    /**
     * Maximum nesting depth of parallel gateways in current execution
     * 0 = sequential, 1 = simple parallel, 2+ = nested parallel
     */
    private int maxNestingDepth;

    /**
     * Gateway-specific completion info
     * Maps gateway ID to its completion status
     */
    private java.util.Map<String, GatewayCompletionInfo> gateways;

    /**
     * Completion info for a specific gateway
     */
    @Data
    @Builder
    public static class GatewayCompletionInfo {
        private String gatewayId;
        private String gatewayType; // parallelGateway, inclusiveGateway
        private int totalIncoming; // Total expected incoming flows
        private int completedIncoming; // How many have arrived
        private boolean satisfied; // Whether join condition is met
    }
}
