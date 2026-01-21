package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.ProcessTemplateForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessTemplateFormRepository extends JpaRepository<ProcessTemplateForm, UUID> {

    List<ProcessTemplateForm> findByProcessTemplateId(UUID processTemplateId);

    Optional<ProcessTemplateForm> findByProcessTemplateIdAndTaskKey(UUID processTemplateId, String taskKey);

    void deleteByProcessTemplateId(UUID processTemplateId);
}
