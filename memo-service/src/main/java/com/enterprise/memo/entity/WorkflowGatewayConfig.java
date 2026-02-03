package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores configuration for parallel/inclusive gateways.
 * Allows admins to configure how parallel branches complete:
 * - ALL: All branches must complete (default)
 * - ANY: Continue when any single branch completes
 * - N_OF_M: Continue when N of M branches complete
 */
@Entity
@Table(name = "workflow_gateway_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The topic this gateway config belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private MemoTopic topic;

    /**
     * Gateway element ID from BPMN XML (e.g., "Gateway_xyz123")
     */
    @Column(name = "gateway_id", nullable = false)
    private String gatewayId;

    /**
     * Human-readable name for the gateway
     */
    @Column(name = "gateway_name")
    private String gatewayName;

    /**
     * Type of gateway: PARALLEL, INCLUSIVE, or EXCLUSIVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false)
    @Builder.Default
    private GatewayType gatewayType = GatewayType.PARALLEL;

    /**
     * Completion mode determines when parallel execution can proceed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_mode", nullable = false)
    @Builder.Default
    private CompletionMode completionMode = CompletionMode.ALL;

    /**
     * For N_OF_M mode: minimum number of branches required
     */
    @Column(name = "minimum_required")
    @Builder.Default
    private Integer minimumRequired = 1;

    /**
     * Total number of incoming flows to this join gateway
     * (used to calculate N of M percentages)
     */
    @Column(name = "total_incoming_flows")
    private Integer totalIncomingFlows;

    /**
     * Optional description for admin reference
     */
    @Column(name = "description")
    private String description;

    /**
     * Whether non-completing branches should be cancelled
     * when completion condition is met (for ANY/N_OF_M modes)
     */
    @Column(name = "cancel_remaining")
    @Builder.Default
    private Boolean cancelRemaining = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Gateway types supported
     */
    public enum GatewayType {
        PARALLEL, // All outgoing paths executed
        INCLUSIVE, // One or more paths based on conditions
        EXCLUSIVE // Exactly one path based on conditions
    }

    /**
     * Completion modes for join gateways
     */
    public enum CompletionMode {
        ALL, // Wait for all incoming branches
        ANY, // Continue when any one branch arrives
        N_OF_M // Continue when minimumRequired branches arrive
    }

    /**
     * Check if this is for a join gateway (has multiple incoming)
     */
    public boolean isJoinGateway() {
        return totalIncomingFlows != null && totalIncomingFlows > 1;
    }

    /**
     * Check if completion condition should be injected into BPMN
     */
    public boolean requiresCompletionCondition() {
        return completionMode != CompletionMode.ALL && isJoinGateway();
    }
}
