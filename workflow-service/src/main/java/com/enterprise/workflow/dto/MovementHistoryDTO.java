package com.enterprise.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Movement history for a process instance, built from ActionTimeline.
 * Used to provide history-based back navigation (return points) rather
 * than relying on static BPMN design.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovementHistoryDTO {

    private String processInstanceId;

    /** Ordered list of completed step entries (oldest first) */
    private List<HistoryEntry> history;

    /** Valid return points derived from history */
    private List<ReturnPoint> returnPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryEntry {
        private String taskId;
        private String taskKey;
        private String taskName;
        private String action; // e.g., "Approve", "Reject"
        private String actorId;
        private String actorName;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnPoint {
        private String taskKey; // BPMN activity ID to return to
        private String taskName; // Human-readable label
        private String label; // e.g., "Return to RM Review"
        private String lastActorId; // UUID of who completed this step (for auto-assignment)
        private String lastActorName; // Who completed this step
        private LocalDateTime lastCompletedAt;
    }
}
