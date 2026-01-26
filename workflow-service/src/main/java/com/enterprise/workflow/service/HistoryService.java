package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.TimelineEventDTO;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.VariableAudit;
import com.enterprise.workflow.repository.ActionTimelineRepository;
import com.enterprise.workflow.repository.VariableAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for workflow history and audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HistoryService {

    private final ActionTimelineRepository actionTimelineRepository;
    private final VariableAuditRepository variableAuditRepository;

    /**
     * Get complete timeline for a process instance.
     */
    public List<TimelineEventDTO> getProcessTimeline(String processInstanceId) {
        List<ActionTimeline> events = actionTimelineRepository
                .findByProcessInstanceIdOrderByCreatedAtAsc(processInstanceId);

        return events.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get paginated timeline for a process instance.
     */
    public Page<TimelineEventDTO> getProcessTimelinePaged(String processInstanceId, Pageable pageable) {
        return actionTimelineRepository
                .findByProcessInstanceIdOrderByCreatedAtDesc(processInstanceId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get timeline filtered by action type.
     */
    public List<TimelineEventDTO> getTimelineByActionType(String processInstanceId,
            ActionTimeline.ActionType actionType) {
        return actionTimelineRepository
                .findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(processInstanceId, actionType)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get variable history for a process instance.
     */
    public List<VariableAudit> getVariableHistory(String processInstanceId) {
        return variableAuditRepository.findByProcessInstanceIdOrderByChangedAtAsc(processInstanceId);
    }

    /**
     * Get history for a specific variable.
     */
    public List<VariableAudit> getVariableHistory(String processInstanceId, String variableName) {
        return variableAuditRepository
                .findByProcessInstanceIdAndVariableNameOrderByChangedAtAsc(processInstanceId, variableName);
    }

    /**
     * Record a variable change for audit.
     */
    @Transactional
    public void recordVariableChange(String processInstanceId, String taskId, String variableName,
            String variableType, Object oldValue, Object newValue,
            UUID changedBy, String changedByName, String reason) {
        VariableAudit audit = VariableAudit.builder()
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .variableName(variableName)
                .variableType(variableType)
                .oldValue(oldValue != null ? Map.of("value", oldValue) : null)
                .newValue(newValue != null ? Map.of("value", newValue) : null)
                .changedBy(changedBy)
                .changedByName(changedByName)
                .changeReason(reason)
                .build();

        variableAuditRepository.save(audit);
        log.debug("Recorded variable change: {} in process {}", variableName, processInstanceId);
    }

    /**
     * Record a timeline event.
     */
    @Transactional
    public void recordTimelineEvent(String processInstanceId, ActionTimeline.ActionType actionType,
            String taskId, String taskName,
            UUID actorId, String actorName, List<String> actorRoles,
            Map<String, Object> metadata, String ipAddress) {
        ActionTimeline event = ActionTimeline.builder()
                .processInstanceId(processInstanceId)
                .actionType(actionType)
                .taskId(taskId)
                .taskName(taskName)
                .actorId(actorId)
                .actorName(actorName)
                .actorRoles(actorRoles)
                .metadata(metadata)
                .ipAddress(ipAddress)
                .build();

        actionTimelineRepository.save(event);
        log.debug("Recorded timeline event: {} in process {}", actionType, processInstanceId);
    }

    private TimelineEventDTO toDTO(ActionTimeline event) {
        return TimelineEventDTO.builder()
                .id(event.getId())
                .processInstanceId(event.getProcessInstanceId())
                .actionType(event.getActionType())
                .taskId(event.getTaskId())
                .taskName(event.getTaskName())
                .actorId(event.getActorId())
                .actorName(event.getActorName())
                .actorRoles(event.getActorRoles())
                .metadata(event.getMetadata())
                .ipAddress(event.getIpAddress())
                .createdAt(event.getCreatedAt())
                .description(generateDescription(event))
                .build();
    }

    private String generateDescription(ActionTimeline event) {
        String actor = event.getActorName() != null ? event.getActorName() : "System";

        return switch (event.getActionType()) {
            case PROCESS_STARTED -> actor + " started the process";
            case PROCESS_COMPLETED -> "Process completed";
            case PROCESS_CANCELLED -> actor + " cancelled the process";
            case TASK_CREATED -> "Task '" + event.getTaskName() + "' was created";
            case TASK_ASSIGNED -> actor + " was assigned to task '" + event.getTaskName() + "'";
            case TASK_CLAIMED -> actor + " claimed task '" + event.getTaskName() + "'";
            case TASK_COMPLETED -> actor + " completed task '" + event.getTaskName() + "'";
            case TASK_DELEGATED -> actor + " delegated task '" + event.getTaskName() + "'";
            case VARIABLE_UPDATED -> actor + " updated a variable";
            case FORM_SUBMITTED -> actor + " submitted a form";
            case DOCUMENT_UPLOADED -> actor + " uploaded a document";
            case COMMENT_ADDED -> actor + " added a comment";
            case INTEGRATION_CALLED -> "External integration was called";
            case NOTIFICATION_SENT -> "Notification was sent";
            case TASK_SENT_BACK -> actor + " sent the task back";
            case TASK_REJECTED -> actor + " rejected the task";
            default -> "Action performed";
        };
    }
}
