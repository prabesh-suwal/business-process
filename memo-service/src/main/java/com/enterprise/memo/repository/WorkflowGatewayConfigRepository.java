package com.enterprise.memo.repository;

import com.enterprise.memo.entity.WorkflowGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for gateway configurations.
 */
@Repository
public interface WorkflowGatewayConfigRepository extends JpaRepository<WorkflowGatewayConfig, UUID> {

    /**
     * Find all gateway configs for a topic.
     */
    List<WorkflowGatewayConfig> findByTopicId(UUID topicId);

    /**
     * Find a specific gateway config.
     */
    Optional<WorkflowGatewayConfig> findByTopicIdAndGatewayId(UUID topicId, String gatewayId);

    /**
     * Find all non-ALL completion mode configs for a topic.
     * These need completion conditions injected into BPMN.
     */
    List<WorkflowGatewayConfig> findByTopicIdAndCompletionModeNot(
            UUID topicId,
            WorkflowGatewayConfig.CompletionMode completionMode);

    /**
     * Delete all configs for a topic (when workflow changes).
     */
    void deleteByTopicId(UUID topicId);
}
