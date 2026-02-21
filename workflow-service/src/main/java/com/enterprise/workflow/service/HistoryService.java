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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
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
     * Computes duration for decision events by pairing them with TASK_CREATED
     * events.
     */
    public List<TimelineEventDTO> getProcessTimeline(String processInstanceId) {
        List<ActionTimeline> events = actionTimelineRepository
                .findByProcessInstanceIdOrderByCreatedAtAsc(processInstanceId);

        // Build a map of taskName → most recent creation time for duration calculation
        // Also track by taskDefinitionKey (from metadata) since names can be null
        Map<String, LocalDateTime> taskCreationByName = new HashMap<>();
        Map<String, LocalDateTime> taskCreationByKey = new HashMap<>();
        for (ActionTimeline e : events) {
            if (e.getActionType() == ActionTimeline.ActionType.TASK_CREATED) {
                if (e.getTaskName() != null) {
                    taskCreationByName.put(e.getTaskName(), e.getCreatedAt());
                }
                // Also index by taskDefinitionKey from metadata
                if (e.getMetadata() != null && e.getMetadata().get("taskDefinitionKey") != null) {
                    taskCreationByKey.put(e.getMetadata().get("taskDefinitionKey").toString(), e.getCreatedAt());
                }
            }
        }

        // ── Two-pass approach for active task tracking ──
        // Pass 1: Process all events and record the active task snapshot after each
        // event
        java.util.LinkedHashSet<String> activeTasks = new java.util.LinkedHashSet<>();
        List<java.util.LinkedHashSet<String>> activeStateAtIndex = new java.util.ArrayList<>();

        for (ActionTimeline event : events) {
            switch (event.getActionType()) {
                case TASK_CREATED:
                    if (event.getTaskName() != null)
                        activeTasks.add(event.getTaskName());
                    break;
                case TASK_COMPLETED:
                case TASK_REJECTED:
                case TASK_SENT_BACK:
                case TASK_CANCELLED:
                    if (event.getTaskName() != null)
                        activeTasks.remove(event.getTaskName());
                    break;
                default:
                    break;
            }
            activeStateAtIndex.add(new java.util.LinkedHashSet<>(activeTasks));
        }

        // Pass 2: Build DTOs. For decision/start events, find the "settled" active
        // state
        // by looking forward past side-effect events (TASK_CREATED, TASK_CANCELLED)
        // within 5s.
        List<TimelineEventDTO> result = new java.util.ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            ActionTimeline event = events.get(i);
            TimelineEventDTO dto = toDTO(event, taskCreationByName, taskCreationByKey);

            if (isDecisionEvent(event.getActionType())
                    || event.getActionType() == ActionTimeline.ActionType.PROCESS_STARTED
                    || event.getActionType() == ActionTimeline.ActionType.MEMO_SUBMITTED) {

                // Find the settled index: look forward past TASK_CREATED/TASK_CANCELLED within
                // 5s
                int settledIdx = i;
                LocalDateTime eventTime = event.getCreatedAt();
                if (eventTime != null) {
                    for (int j = i + 1; j < events.size(); j++) {
                        ActionTimeline next = events.get(j);
                        if (next.getCreatedAt() == null)
                            break;
                        long diffSeconds = Duration.between(eventTime, next.getCreatedAt()).toSeconds();
                        if (diffSeconds > 5)
                            break;
                        // Only settle past side-effect events, stop at real user actions
                        if (next.getActionType() == ActionTimeline.ActionType.TASK_CREATED
                                || next.getActionType() == ActionTimeline.ActionType.TASK_CANCELLED) {
                            settledIdx = j;
                        } else {
                            break;
                        }
                    }
                }

                java.util.Set<String> settled = activeStateAtIndex.get(settledIdx);
                if (!settled.isEmpty()) {
                    dto.setNextSteps(new java.util.ArrayList<>(settled));
                } else {
                    dto.setNextSteps(findTerminalState(events, i));
                }
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Check if the process ended after the given event index.
     */
    private List<String> findTerminalState(List<ActionTimeline> events, int fromIndex) {
        for (int j = fromIndex + 1; j < events.size(); j++) {
            ActionTimeline next = events.get(j);
            if (next.getActionType() == ActionTimeline.ActionType.PROCESS_COMPLETED) {
                return List.of("Process Completed");
            } else if (next.getActionType() == ActionTimeline.ActionType.PROCESS_CANCELLED) {
                return List.of("Process Cancelled");
            }
        }
        return List.of();
    }

    /**
     * Get paginated timeline for a process instance.
     */
    public Page<TimelineEventDTO> getProcessTimelinePaged(String processInstanceId, Pageable pageable) {
        return actionTimelineRepository
                .findByProcessInstanceIdOrderByCreatedAtDesc(processInstanceId, pageable)
                .map(event -> toDTO(event, Map.of(), Map.of()));
    }

    /**
     * Get timeline filtered by action type.
     */
    public List<TimelineEventDTO> getTimelineByActionType(String processInstanceId,
            ActionTimeline.ActionType actionType) {
        return actionTimelineRepository
                .findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(processInstanceId, actionType)
                .stream()
                .map(event -> toDTO(event, Map.of(), Map.of()))
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

    // ─── DTO Mapping ────────────────────────────────────────────────────

    private TimelineEventDTO toDTO(ActionTimeline event,
            Map<String, LocalDateTime> taskCreationByName,
            Map<String, LocalDateTime> taskCreationByKey) {
        // Compute duration for decision events
        Long durationMs = null;
        if (isDecisionEvent(event.getActionType()) && event.getCreatedAt() != null) {
            // Try matching by taskName first
            LocalDateTime creationTime = null;
            if (event.getTaskName() != null) {
                creationTime = taskCreationByName.get(event.getTaskName());
            }
            // Fallback: match by taskDefinitionKey from metadata
            if (creationTime == null && event.getMetadata() != null
                    && event.getMetadata().get("taskDefinitionKey") != null) {
                creationTime = taskCreationByKey.get(
                        event.getMetadata().get("taskDefinitionKey").toString());
            }
            if (creationTime != null) {
                long ms = Duration.between(creationTime, event.getCreatedAt()).toMillis();
                if (ms >= 0) {
                    durationMs = ms;
                }
            }
        }

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
                .actionLabel(extractActionLabel(event))
                .durationMs(durationMs)
                .build();
    }

    private boolean isDecisionEvent(ActionTimeline.ActionType type) {
        return type == ActionTimeline.ActionType.TASK_COMPLETED
                || type == ActionTimeline.ActionType.TASK_SENT_BACK
                || type == ActionTimeline.ActionType.TASK_REJECTED
                || type == ActionTimeline.ActionType.TASK_CANCELLED;
    }

    /**
     * Extract the action label from metadata or derive from action type.
     */
    private String extractActionLabel(ActionTimeline event) {
        Map<String, Object> meta = event.getMetadata();
        if (meta != null) {
            if (meta.containsKey("actionLabel")) {
                return meta.get("actionLabel").toString();
            }
            if (meta.containsKey("action")) {
                return meta.get("action").toString();
            }
        }

        return switch (event.getActionType()) {
            case TASK_COMPLETED -> "Approved";
            case TASK_SENT_BACK -> "Sent Back";
            case TASK_REJECTED -> "Rejected";
            case TASK_CANCELLED -> "Cancelled";
            case TASK_CLAIMED -> "Claimed";
            case TASK_ASSIGNED -> "Assigned";
            case PROCESS_STARTED -> "Started";
            case PROCESS_COMPLETED -> "Completed";
            case PROCESS_CANCELLED -> "Cancelled";
            case MEMO_SUBMITTED -> "Submitted";
            default -> null;
        };
    }

    private String generateDescription(ActionTimeline event) {
        String actor = event.getActorName() != null ? event.getActorName() : "System";

        return switch (event.getActionType()) {
            case PROCESS_STARTED -> actor + " started the process";
            case PROCESS_COMPLETED -> "Process completed";
            case PROCESS_CANCELLED -> actor + " cancelled the process";
            case MEMO_SUBMITTED -> actor + " submitted the memo";
            case TASK_CREATED -> "Task '" + event.getTaskName() + "' was created";
            case TASK_ASSIGNED -> actor + " was assigned to '" + event.getTaskName() + "'";
            case TASK_CLAIMED -> actor + " claimed '" + event.getTaskName() + "'";
            case TASK_COMPLETED -> actor + " completed '" + event.getTaskName() + "'";
            case TASK_DELEGATED -> actor + " delegated '" + event.getTaskName() + "'";
            case VARIABLE_UPDATED -> actor + " updated a variable";
            case FORM_SUBMITTED -> actor + " submitted a form";
            case DOCUMENT_UPLOADED -> actor + " uploaded a document";
            case COMMENT_ADDED -> actor + " added a comment";
            case INTEGRATION_CALLED -> "External integration was called";
            case NOTIFICATION_SENT -> "Notification was sent";
            case TASK_SENT_BACK -> actor + " sent back '" + event.getTaskName() + "'";
            case TASK_REJECTED -> actor + " rejected '" + event.getTaskName() + "'";
            case TASK_CANCELLED -> "'" + event.getTaskName() + "' was cancelled";
            default -> "Action performed";
        };
    }
}
