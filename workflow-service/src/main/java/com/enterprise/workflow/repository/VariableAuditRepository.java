package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.VariableAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VariableAuditRepository extends JpaRepository<VariableAudit, UUID> {

    List<VariableAudit> findByProcessInstanceIdOrderByChangedAtAsc(String processInstanceId);

    List<VariableAudit> findByProcessInstanceIdAndVariableNameOrderByChangedAtAsc(
            String processInstanceId, String variableName);

    List<VariableAudit> findByTaskIdOrderByChangedAtAsc(String taskId);
}
