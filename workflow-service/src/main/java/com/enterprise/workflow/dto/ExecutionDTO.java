package com.enterprise.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO representing a process execution (token).
 * Used for tracking parallel branches in workflows.
 * 
 * In BPMN parallel gateways, each branch gets its own execution.
 * Deep nested parallel gateways create a hierarchy of executions.
 */
@Data
@Builder
public class ExecutionDTO {

    /**
     * Unique execution ID
     */
    private String executionId;

    /**
     * Parent execution ID (for nested parallel tracking)
     * Null for root execution
     */
    private String parentExecutionId;

    /**
     * Process instance ID this execution belongs to
     */
    private String processInstanceId;

    /**
     * Current activity (task/gateway) ID
     */
    private String activityId;

    /**
     * Current activity name (for display)
     */
    private String activityName;

    /**
     * Type of activity: userTask, exclusiveGateway, parallelGateway, etc.
     */
    private String activityType;

    /**
     * Whether this execution is currently active
     */
    private boolean active;

    /**
     * Whether this execution has ended
     */
    private boolean ended;

    /**
     * Whether this is a scope execution (container for child executions)
     */
    private boolean scope;

    /**
     * Nesting depth for deep nested parallel tracking
     * 0 = root, 1 = first level parallel, 2 = nested parallel inside parallel, etc.
     */
    private int nestingLevel;

    /**
     * Child executions (for tree representation of deep nested parallels)
     */
    private List<ExecutionDTO> childExecutions;
}
