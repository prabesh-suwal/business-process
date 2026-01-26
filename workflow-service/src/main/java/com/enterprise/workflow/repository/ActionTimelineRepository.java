package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.ActionTimeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionTimelineRepository extends JpaRepository<ActionTimeline, UUID> {

        List<ActionTimeline> findByProcessInstanceIdOrderByCreatedAtAsc(String processInstanceId);

        Page<ActionTimeline> findByProcessInstanceIdOrderByCreatedAtDesc(
                        String processInstanceId, Pageable pageable);

        List<ActionTimeline> findByProcessInstanceIdOrderByCreatedAtDesc(String processInstanceId);

        List<ActionTimeline> findByActorIdOrderByCreatedAtDesc(UUID actorId);

        List<ActionTimeline> findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(
                        String processInstanceId, ActionTimeline.ActionType actionType);
}
