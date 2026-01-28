package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoTaskRepository extends JpaRepository<MemoTask, UUID> {

        Optional<MemoTask> findByWorkflowTaskId(String workflowTaskId);

        List<MemoTask> findByMemoIdOrderByCreatedAtDesc(UUID memoId);

        List<MemoTask> findByAssignedToAndStatus(String assignedTo, MemoTask.TaskStatus status);

        @Query("SELECT t FROM MemoTask t WHERE t.status = 'PENDING' AND " +
                        "(t.assignedTo = :userId OR t.candidateUsers LIKE %:userId% OR " +
                        "t.candidateGroups IN :groups)")
        List<MemoTask> findInboxTasks(String userId, List<String> groups);

        @Query("SELECT t FROM MemoTask t WHERE t.status = 'PENDING' AND t.assignedTo IS NULL AND " +
                        "(t.candidateUsers LIKE %:userId% OR t.candidateGroups IN :groups)")
        List<MemoTask> findClaimableTasks(String userId, List<String> groups);

        List<MemoTask> findByMemoId(UUID memoId);

        /**
         * Find all tasks where user is involved (assigned, candidate, or completed).
         * Used for determining memo access/visibility.
         */
        @Query("SELECT t FROM MemoTask t WHERE " +
                        "t.assignedTo = :userId OR " +
                        "t.candidateUsers LIKE %:userId% OR " +
                        "(t.candidateGroups IS NOT NULL AND t.candidateGroups IN :groups)")
        List<MemoTask> findTasksForUser(String userId, List<String> groups);
}
