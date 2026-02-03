package com.enterprise.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for parallel execution status of a memo workflow.
 * Mirrors workflow-service ParallelExecutionStatusDTO with memo-specific
 * additions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParallelExecutionStatusDTO {

    /**
     * Process instance ID
     */
    private String processInstanceId;

    /**
     * Whether the workflow currently has parallel executions
     */
    private boolean isInParallelExecution;

    /**
     * Total number of parallel branches
     */
    private int totalBranches;

    /**
     * Number of branches that have completed
     */
    private int completedBranches;

    /**
     * Number of branches still active
     */
    private int activeBranches;

    /**
     * Currently active tasks across all parallel branches
     */
    private List<ActiveTaskInfo> activeTasks;

    /**
     * Maximum nesting depth of parallel gateways
     */
    private int maxNestingDepth;

    /**
     * Human-readable status message
     * e.g., "Parallel approval in progress: 2 of 3 branches completed"
     */
    private String statusMessage;

    /**
     * Progress percentage (0-100)
     */
    private int progressPercent;

    /**
     * Gateway completion mode from config (ALL, ANY, N_OF_M)
     * Used to determine if early completion with cancellation should happen.
     */
    private String completionMode;

    /**
     * The gateway ID that this parallel execution is associated with
     */
    private String gatewayId;

    /**
     * Required completions for N_OF_M mode
     */
    private Integer requiredCompletions;

    /**
     * Information about an active task in a parallel branch
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveTaskInfo {
        private String taskId;
        private String taskName;
        private String taskDefinitionKey;
        private String assignee;
        private String assigneeName;
        private String branchName; // e.g., "Branch A", "Finance Review"
        private int branchIndex; // 0, 1, 2...
        private boolean isCurrent; // Is this the user's current task?
    }

    /**
     * Create a status message based on the parallel execution state
     */
    public String generateStatusMessage() {
        if (!isInParallelExecution) {
            return "Sequential workflow";
        }
        if (completedBranches == 0) {
            return String.format("Parallel approval started: %d branches active", activeBranches);
        }
        if (completedBranches == totalBranches) {
            return "All parallel branches completed";
        }
        return String.format("Parallel approval in progress: %d of %d completed",
                completedBranches, totalBranches);
    }

    /**
     * Calculate progress percentage
     */
    public int calculateProgress() {
        if (totalBranches == 0)
            return 100;
        return (completedBranches * 100) / totalBranches;
    }
}
