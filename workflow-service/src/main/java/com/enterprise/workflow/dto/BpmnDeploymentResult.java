package com.enterprise.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result of deploying BPMN to Flowable.
 * Contains both the Flowable process definition ID and our ProcessTemplate
 * UUID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmnDeploymentResult {

    /**
     * Flowable process definition ID (e.g., "Process_1:23:abc123").
     * Used to start process instances in Flowable.
     */
    private String processDefinitionId;

    /**
     * Our internal ProcessTemplate UUID.
     * Used for centralized assignment/viewer resolution.
     */
    private UUID processTemplateId;

    /**
     * Flowable deployment ID.
     */
    private String deploymentId;

    /**
     * Process key (normalized process name).
     */
    private String processKey;
}
