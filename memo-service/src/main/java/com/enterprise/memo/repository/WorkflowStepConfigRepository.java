package com.enterprise.memo.repository;

import com.enterprise.memo.entity.WorkflowStepConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepConfigRepository extends JpaRepository<WorkflowStepConfig, UUID> {

    List<WorkflowStepConfig> findByMemoTopicIdOrderByStepOrder(UUID memoTopicId);

    List<WorkflowStepConfig> findByMemoTopicId(UUID memoTopicId);

    Optional<WorkflowStepConfig> findByMemoTopicIdAndTaskKey(UUID memoTopicId, String taskKey);

    void deleteByMemoTopicId(UUID memoTopicId);
}
