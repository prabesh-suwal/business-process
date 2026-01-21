package com.enterprise.form.repository;

import com.enterprise.form.entity.FormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    List<FormSubmission> findByProcessInstanceIdOrderBySubmittedAtAsc(String processInstanceId);

    Optional<FormSubmission> findByTaskId(String taskId);

    List<FormSubmission> findByFormDefinitionIdOrderBySubmittedAtDesc(UUID formDefinitionId);

    Page<FormSubmission> findBySubmittedByOrderBySubmittedAtDesc(UUID submittedBy, Pageable pageable);
}
