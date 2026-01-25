package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.WorkflowVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVariableRepository extends JpaRepository<WorkflowVariable, UUID> {
    Optional<WorkflowVariable> findByKey(String key);
}
