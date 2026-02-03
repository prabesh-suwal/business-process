package com.enterprise.memo.dto;

import com.enterprise.memo.entity.MemoTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MemoTaskDTO {
    private UUID id;
    private UUID memoId;
    private String memoNumber;
    private String memoSubject;
    private String memoTopicName;
    private String workflowTaskId;
    private String taskDefinitionKey;
    private String taskName;
    private String stage;
    private String assignedTo;
    private String assignedToName;
    private String status;
    private String actionTaken;
    private Integer priority;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;
    private String candidateGroups;
    private String candidateUsers;

    public static MemoTaskDTO fromEntity(MemoTask task) {
        return MemoTaskDTO.builder()
                .id(task.getId())
                .memoId(task.getMemo().getId())
                .memoNumber(task.getMemo().getMemoNumber())
                .memoSubject(task.getMemo().getSubject())
                .memoTopicName(task.getMemo().getTopic().getName())
                .workflowTaskId(task.getWorkflowTaskId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .taskName(task.getTaskName())
                .stage(task.getStage())
                .assignedTo(task.getAssignedTo())
                .assignedToName(task.getAssignedToName())
                .status(task.getStatus().name())
                .actionTaken(task.getActionTaken() != null ? task.getActionTaken().name() : null)
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .claimedAt(task.getClaimedAt())
                .completedAt(task.getCompletedAt())
                .candidateGroups(task.getCandidateGroups())
                .candidateUsers(task.getCandidateUsers())
                .build();
    }
}
