package com.enterprise.memo.service;

import com.enterprise.memo.client.WorkflowClient;
import com.enterprise.memo.entity.*;
import com.enterprise.memo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing memo tasks.
 * This is where memo-service orchestrates task lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemoTaskService {

    private final MemoTaskRepository taskRepository;
    private final MemoCommentRepository commentRepository;
    private final MemoVersionRepository versionRepository;
    private final MemoRepository memoRepository;
    private final MemoTopicRepository topicRepository;
    private final WorkflowClient workflowClient;

    /**
     * Called when workflow creates a task - syncs to MemoTask.
     */
    public MemoTask onTaskCreated(String workflowTaskId, UUID memoId, String taskDefKey,
            String taskName, List<String> candidateGroups,
            List<String> candidateUsers) {
        log.info("Task created: {} for memo {}", workflowTaskId, memoId);

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        MemoTask task = MemoTask.builder()
                .memo(memo)
                .workflowTaskId(workflowTaskId)
                .taskDefinitionKey(taskDefKey)
                .taskName(taskName)
                .stage(taskDefKey)
                .candidateGroups(candidateGroups != null ? String.join(",", candidateGroups) : null)
                .candidateUsers(candidateUsers != null ? String.join(",", candidateUsers) : null)
                .status(MemoTask.TaskStatus.PENDING)
                .build();

        task = taskRepository.save(task);

        // Update memo current stage
        memo.setCurrentStage(taskDefKey);
        memoRepository.save(memo);

        return task;
    }

    /**
     * Save a task (used by webhook handlers).
     */
    public MemoTask saveTask(MemoTask task) {
        return taskRepository.save(task);
    }

    /**
     * Get task by workflow task ID.
     */
    @Transactional(readOnly = true)
    public Optional<MemoTask> getByWorkflowTaskId(String workflowTaskId) {
        return taskRepository.findByWorkflowTaskId(workflowTaskId);
    }

    /**
     * Get all tasks for a memo.
     */
    @Transactional(readOnly = true)
    public List<MemoTask> getTasksForMemo(UUID memoId) {
        return taskRepository.findByMemoIdOrderByCreatedAtDesc(memoId);
    }

    /**
     * Get the Flowable process instance ID for a memo.
     * Used for parallel execution tracking.
     */
    @Transactional(readOnly = true)
    public String getProcessInstanceIdForMemo(UUID memoId) {
        return memoRepository.findById(memoId)
                .map(Memo::getProcessInstanceId)
                .orElse(null);
    }

    /**
     * Claim a task.
     */
    public MemoTask claimTask(UUID taskId, String userId, String userName) {
        MemoTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() != MemoTask.TaskStatus.PENDING) {
            throw new RuntimeException("Task is not in PENDING status");
        }

        task.setAssignedTo(userId);
        task.setAssignedToName(userName);
        task.setStatus(MemoTask.TaskStatus.CLAIMED);
        log.info("Claiming task {} for user {}", taskId, userId);
        task.setClaimedAt(LocalDateTime.now());

        // Also claim in workflow
        workflowClient.claimTask(task.getWorkflowTaskId(), userId, userName != null ? userName : "Unknown");

        return taskRepository.save(task);
    }

    /**
     * Complete a task with action.
     */
    public MemoTask completeTask(UUID taskId, MemoTask.TaskAction action, String comment,
            UUID userId, String userName, Map<String, Object> variables) {
        MemoTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() == MemoTask.TaskStatus.COMPLETED) {
            throw new RuntimeException("Task is already completed");
        }

        // Save comment
        if (comment != null && !comment.isBlank()) {
            MemoComment memoComment = MemoComment.builder()
                    .memo(task.getMemo())
                    .task(task)
                    .userId(userId)
                    .userName(userName)
                    .content(comment)
                    .type(mapActionToCommentType(action))
                    .build();
            commentRepository.save(memoComment);
        }

        // Update task
        task.setActionTaken(action);
        task.setComments(comment);
        task.setStatus(MemoTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        // Create version snapshot
        createVersionSnapshot(task.getMemo(), userId, userName, action.name());

        // Complete in workflow
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("action", action.name());
        taskVariables.put("approved", action == MemoTask.TaskAction.APPROVE);
        taskVariables.put("memoId", task.getMemo().getId().toString());
        taskVariables.put("action", action); // This seems to be a duplicate of the first "action" but with enum type
        taskVariables.put("comment", comment);
        if (variables != null) {
            taskVariables.putAll(variables);
        }

        workflowClient.completeTask(task.getWorkflowTaskId(), userId.toString(),
                userName != null ? userName : "Unknown",
                taskVariables, false); // cancelOthers handled by TaskController

        // Update memo status based on action
        updateMemoStatus(task.getMemo(), action);

        log.info("Task {} completed with action {}", taskId, action);

        return taskRepository.save(task);
    }

    /**
     * Get inbox tasks for a user.
     */
    @Transactional(readOnly = true)
    public List<MemoTask> getInboxTasks(String userId, List<String> groups) {
        return taskRepository.findInboxTasks(userId, groups);
    }

    private MemoComment.CommentType mapActionToCommentType(MemoTask.TaskAction action) {
        return switch (action) {
            case APPROVE -> MemoComment.CommentType.APPROVAL;
            case REJECT -> MemoComment.CommentType.REJECTION;
            case CLARIFY -> MemoComment.CommentType.CLARIFICATION;
            default -> MemoComment.CommentType.COMMENT;
        };
    }

    private void createVersionSnapshot(Memo memo, UUID userId, String userName, String action) {
        int versionCount = versionRepository.countByMemoId(memo.getId());

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("subject", memo.getSubject());
        snapshot.put("formData", memo.getFormData());
        snapshot.put("content", memo.getContent());

        MemoVersion version = MemoVersion.builder()
                .memo(memo)
                .version(versionCount + 1)
                .dataSnapshot(snapshot)
                .statusAtSnapshot(memo.getStatus().name())
                .stageAtSnapshot(memo.getCurrentStage())
                .createdBy(userId)
                .createdByName(userName)
                .action(action)
                .build();

        versionRepository.save(version);
    }

    private void updateMemoStatus(Memo memo, MemoTask.TaskAction action) {
        if (action == MemoTask.TaskAction.REJECT) {
            memo.setStatus(MemoStatus.REJECTED);
            memoRepository.save(memo);
        }
        // Other status updates happen via workflow completion listener
    }
}
