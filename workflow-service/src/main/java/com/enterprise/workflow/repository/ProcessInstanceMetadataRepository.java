package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.ProcessInstanceMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessInstanceMetadataRepository extends JpaRepository<ProcessInstanceMetadata, UUID> {

    Optional<ProcessInstanceMetadata> findByFlowableProcessInstanceId(String flowableProcessInstanceId);

    Page<ProcessInstanceMetadata> findByProductIdOrderByStartedAtDesc(UUID productId, Pageable pageable);

    Page<ProcessInstanceMetadata> findByProductIdAndStatusOrderByStartedAtDesc(
            UUID productId, ProcessInstanceMetadata.ProcessInstanceStatus status, Pageable pageable);

    Page<ProcessInstanceMetadata> findByStartedByOrderByStartedAtDesc(UUID startedBy, Pageable pageable);
}
