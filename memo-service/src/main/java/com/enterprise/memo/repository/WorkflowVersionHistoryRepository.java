package com.enterprise.memo.repository;

import com.enterprise.memo.entity.WorkflowVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVersionHistoryRepository extends JpaRepository<WorkflowVersionHistory, UUID> {

    /**
     * Find all versions for a topic, ordered by version descending (newest first).
     */
    List<WorkflowVersionHistory> findByTopicIdOrderByVersionDesc(UUID topicId);

    /**
     * Find a specific version of a topic's workflow.
     */
    Optional<WorkflowVersionHistory> findByTopicIdAndVersion(UUID topicId, Integer version);

    /**
     * Find the latest (highest version number) snapshot for a topic.
     */
    Optional<WorkflowVersionHistory> findFirstByTopicIdOrderByVersionDesc(UUID topicId);

    /**
     * Find version by its workflowTemplateId (Flowable process definition ID).
     * Useful for resolving which version a running process belongs to.
     */
    Optional<WorkflowVersionHistory> findByWorkflowTemplateId(String workflowTemplateId);
}
