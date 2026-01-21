package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.WorkflowConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowConfigurationRepository extends JpaRepository<WorkflowConfiguration, UUID> {

    Optional<WorkflowConfiguration> findByCode(String code);

    List<WorkflowConfiguration> findByProductCode(String productCode);

    List<WorkflowConfiguration> findByProductCodeAndActive(String productCode, boolean active);

    List<WorkflowConfiguration> findByActive(boolean active);

    boolean existsByCode(String code);

    Optional<WorkflowConfiguration> findByProcessTemplateId(UUID processTemplateId);
}
