package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.TaskConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskConfigurationRepository extends JpaRepository<TaskConfiguration, UUID> {

    List<TaskConfiguration> findByProcessTemplateIdOrderByTaskOrderAsc(UUID processTemplateId);

    Optional<TaskConfiguration> findByProcessTemplateIdAndTaskKey(UUID processTemplateId, String taskKey);

    void deleteByProcessTemplateId(UUID processTemplateId);

    boolean existsByProcessTemplateIdAndTaskKey(UUID processTemplateId, String taskKey);
}
