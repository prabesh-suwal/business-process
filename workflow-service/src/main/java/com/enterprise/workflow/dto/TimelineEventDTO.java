package com.enterprise.workflow.dto;

import com.enterprise.workflow.entity.ActionTimeline.ActionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEventDTO {

    private UUID id;
    private String processInstanceId;
    private ActionType actionType;
    private String taskId;
    private String taskName;
    private UUID actorId;
    private String actorName;
    private List<String> actorRoles;
    private Map<String, Object> metadata;
    private String ipAddress;
    private LocalDateTime createdAt;

    // Human-readable action description
    private String description;

    // The decision/action label (e.g., "Approved", "Sent Back", "Rejected")
    private String actionLabel;

    // Duration this step took in milliseconds (from task creation to completion)
    private Long durationMs;

    // What step(s) the workflow moved to after this event
    private List<String> nextSteps;
}
