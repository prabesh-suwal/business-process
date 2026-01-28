package com.enterprise.memo.service;

import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoStatus;
import com.enterprise.memo.entity.MemoTask;
import com.enterprise.memo.repository.MemoRepository;
import com.enterprise.memo.repository.MemoTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for determining memo access/visibility.
 * 
 * A user can access a memo if:
 * 1. User is the initiator (createdBy)
 * 2. User was involved in a completed step (from MemoTask history)
 * 3. User is a candidate for the current active step
 * 4. User has explicit viewer access (via ViewerService)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemoAccessService {

    private final MemoRepository memoRepository;
    private final MemoTaskRepository taskRepository;
    private final ViewerService viewerService;

    /**
     * Get all memos the user can access.
     */
    public List<Memo> getAccessibleMemos(String userId, List<String> roles, String departmentId) {
        Set<UUID> accessibleMemoIds = new HashSet<>();

        // 1. Memos created by user
        List<Memo> createdMemos = memoRepository.findByCreatedByOrderByCreatedAtDesc(UUID.fromString(userId));
        for (Memo m : createdMemos) {
            accessibleMemoIds.add(m.getId());
        }
        log.debug("User {} created {} memos", userId, createdMemos.size());

        // 2. Memos where user was involved in any task (completed or current)
        List<MemoTask> userTasks = taskRepository.findTasksForUser(userId, roles);
        for (MemoTask task : userTasks) {
            accessibleMemoIds.add(task.getMemo().getId());
        }
        log.debug("User {} has {} tasks", userId, userTasks.size());

        // 3. Memos with explicit viewer access
        List<UUID> viewableMemoIds = viewerService.getViewableMemos(userId, roles, departmentId);
        accessibleMemoIds.addAll(viewableMemoIds);
        log.debug("User {} has viewer access to {} memos", userId, viewableMemoIds.size());

        // Fetch all accessible memos
        if (accessibleMemoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Memo> memos = memoRepository.findAllById(accessibleMemoIds);

        // Sort by updatedAt descending
        memos.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null)
                return 0;
            if (a.getUpdatedAt() == null)
                return 1;
            if (b.getUpdatedAt() == null)
                return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });

        return memos;
    }

    /**
     * Get memos with access reason for each.
     */
    public List<MemoWithAccess> getAccessibleMemosWithReason(String userId, List<String> roles, String departmentId) {
        List<Memo> memos = getAccessibleMemos(userId, roles, departmentId);

        return memos.stream().map(memo -> {
            AccessReason reason = determineAccessReason(memo, userId, roles, departmentId);
            return new MemoWithAccess(memo, reason);
        }).collect(Collectors.toList());
    }

    /**
     * Determine why user has access to a memo.
     */
    private AccessReason determineAccessReason(Memo memo, String userId, List<String> roles, String departmentId) {
        // Check if initiator
        if (memo.getCreatedBy() != null && memo.getCreatedBy().toString().equals(userId)) {
            return AccessReason.INITIATOR;
        }

        // Check if assigned/candidate for current task
        List<MemoTask> tasks = taskRepository.findByMemoIdOrderByCreatedAtDesc(memo.getId());
        for (MemoTask task : tasks) {
            if (task.getStatus() == MemoTask.TaskStatus.PENDING || task.getStatus() == MemoTask.TaskStatus.CLAIMED) {
                if (isUserCandidate(task, userId, roles)) {
                    return AccessReason.CURRENT_APPROVER;
                }
            }
        }

        // Check if was involved in a completed task
        for (MemoTask task : tasks) {
            if (task.getStatus() == MemoTask.TaskStatus.COMPLETED) {
                if (task.getAssignedTo() != null && task.getAssignedTo().equals(userId)) {
                    return AccessReason.PAST_APPROVER;
                }
            }
        }

        // Check if has viewer access
        if (viewerService.canViewMemo(memo.getId(), userId, roles, departmentId)) {
            return AccessReason.VIEWER;
        }

        return AccessReason.UNKNOWN;
    }

    /**
     * Check if user is a candidate for a task.
     */
    private boolean isUserCandidate(MemoTask task, String userId, List<String> roles) {
        // Check direct user assignment
        if (task.getAssignedTo() != null && task.getAssignedTo().equals(userId)) {
            return true;
        }

        // Check candidate users
        if (task.getCandidateUsers() != null && !task.getCandidateUsers().isEmpty()) {
            String[] candidateUsers = task.getCandidateUsers().split(",");
            for (String cu : candidateUsers) {
                if (cu.trim().equals(userId)) {
                    return true;
                }
            }
        }

        // Check candidate groups (roles)
        if (task.getCandidateGroups() != null && !task.getCandidateGroups().isEmpty() && roles != null) {
            String[] candidateGroups = task.getCandidateGroups().split(",");
            for (String cg : candidateGroups) {
                if (roles.contains(cg.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if user can access a specific memo.
     */
    public boolean canAccessMemo(UUID memoId, String userId, List<String> roles, String departmentId) {
        Memo memo = memoRepository.findById(memoId).orElse(null);
        if (memo == null)
            return false;

        // Is initiator
        if (memo.getCreatedBy() != null && memo.getCreatedBy().toString().equals(userId)) {
            return true;
        }

        // Was involved in any task
        List<MemoTask> tasks = taskRepository.findByMemoIdOrderByCreatedAtDesc(memoId);
        for (MemoTask task : tasks) {
            if (isUserCandidate(task, userId, roles)) {
                return true;
            }
            if (task.getAssignedTo() != null && task.getAssignedTo().equals(userId)) {
                return true;
            }
        }

        // Has viewer access
        return viewerService.canViewMemo(memoId, userId, roles, departmentId);
    }

    public enum AccessReason {
        INITIATOR, // User created the memo
        CURRENT_APPROVER, // User is in current approval step
        PAST_APPROVER, // User was in a previous approval step
        VIEWER, // User has explicit viewer access
        UNKNOWN
    }

    public record MemoWithAccess(Memo memo, AccessReason accessReason) {
    }
}
