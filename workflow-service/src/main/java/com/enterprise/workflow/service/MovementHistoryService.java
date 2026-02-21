package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.MovementHistoryDTO;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.repository.ActionTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Builds movement history for a process instance from ActionTimeline events.
 *
 * Instead of relying on static BPMN paths for back navigation, this service
 * uses the actual movement history to determine valid return points.
 * This correctly handles:
 * - Loops (same task completed multiple times)
 * - Send-backs (returned to a previous step)
 * - Parallel branches (only includes steps in the main execution path)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovementHistoryService {

    private final ActionTimelineRepository actionTimelineRepository;
    private final TaskService taskService;
    private final ProcessAnalyzerService processAnalyzerService;

    /**
     * Get the movement history and valid return points for a task.
     *
     * @param taskId the current active task ID
     * @return movement history with return points
     */
    public MovementHistoryDTO getMovementHistory(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String currentTaskKey = task.getTaskDefinitionKey();
        String processDefinitionId = task.getProcessDefinitionId();

        return buildMovementHistory(processInstanceId, currentTaskKey, task.getName(), processDefinitionId);
    }

    /**
     * Get movement history by process instance ID.
     */
    public MovementHistoryDTO getMovementHistoryByProcessInstance(String processInstanceId) {
        return buildMovementHistory(processInstanceId, null, null, null);
    }

    private MovementHistoryDTO buildMovementHistory(String processInstanceId,
            String currentTaskKey,
            String currentTaskName,
            String processDefinitionId) {
        // Get completed tasks in chronological order
        List<ActionTimeline> completedEvents = actionTimelineRepository
                .findByProcessInstanceIdAndActionTypeOrderByCreatedAtAsc(
                        processInstanceId, ActionTimeline.ActionType.TASK_COMPLETED);

        // Build the ordered history entries
        List<MovementHistoryDTO.HistoryEntry> history = new ArrayList<>();

        for (ActionTimeline event : completedEvents) {
            // Skip cancelled/skipped tasks - they're not real movement
            Map<String, Object> meta = event.getMetadata();
            if (meta != null && Boolean.TRUE.equals(meta.get("skipped"))) {
                continue;
            }

            String taskKey = null;
            String actionLabel = null;

            if (meta != null) {
                taskKey = (String) meta.get("taskDefinitionKey");
                actionLabel = (String) meta.get("action");
            }

            history.add(MovementHistoryDTO.HistoryEntry.builder()
                    .taskId(event.getTaskId())
                    .taskKey(taskKey)
                    .taskName(event.getTaskName())
                    .action(actionLabel != null ? actionLabel
                            : (Boolean.TRUE.equals(meta != null ? meta.get("approved") : null)
                                    ? "Approve"
                                    : "Reject"))
                    .actorId(event.getActorId() != null ? event.getActorId().toString() : null)
                    .actorName(event.getActorName())
                    .timestamp(event.getCreatedAt())
                    .metadata(meta)
                    .build());
        }

        // Add current task as an entry with "current" action
        if (currentTaskKey != null) {
            history.add(MovementHistoryDTO.HistoryEntry.builder()
                    .taskKey(currentTaskKey)
                    .taskName(currentTaskName)
                    .action("current")
                    .build());
        }

        // Build return points from completed steps (deduplicate by taskKey, keep
        // latest)
        // A return point = a previously completed step that we can navigate back to
        Map<String, MovementHistoryDTO.ReturnPoint> returnPointMap = new LinkedHashMap<>();

        for (ActionTimeline event : completedEvents) {
            Map<String, Object> meta = event.getMetadata();
            if (meta != null && Boolean.TRUE.equals(meta.get("skipped")))
                continue;

            String taskKey = meta != null ? (String) meta.get("taskDefinitionKey") : null;
            if (taskKey == null)
                continue;

            // Don't include the current task as a return point
            if (taskKey.equals(currentTaskKey))
                continue;

            returnPointMap.put(taskKey, MovementHistoryDTO.ReturnPoint.builder()
                    .taskKey(taskKey)
                    .taskName(event.getTaskName())
                    .label("Return to " + event.getTaskName())
                    .lastActorId(event.getActorId() != null ? event.getActorId().toString() : null)
                    .lastActorName(event.getActorName())
                    .lastCompletedAt(event.getCreatedAt())
                    .build());
        }

        List<MovementHistoryDTO.ReturnPoint> returnPoints = new ArrayList<>(returnPointMap.values());

        // Gateway-aware filtering: if current task is inside a gateway branch,
        // exclude sibling branch tasks from return points
        if (currentTaskKey != null && processDefinitionId != null) {
            try {
                List<String> validKeys = processAnalyzerService.getValidReturnPointTaskKeys(
                        processDefinitionId, currentTaskKey, completedEvents);
                returnPoints = returnPoints.stream()
                        .filter(rp -> validKeys.contains(rp.getTaskKey()))
                        .toList();
                log.debug("Gateway-filtered return points: {} (from {})",
                        returnPoints.size(), returnPointMap.size());
            } catch (Exception e) {
                log.debug("Could not apply gateway-aware filtering: {}", e.getMessage());
            }
        }

        log.debug("Built movement history for process {}: {} entries, {} return points",
                processInstanceId, history.size(), returnPoints.size());

        return MovementHistoryDTO.builder()
                .processInstanceId(processInstanceId)
                .history(history)
                .returnPoints(returnPoints)
                .build();
    }
}
