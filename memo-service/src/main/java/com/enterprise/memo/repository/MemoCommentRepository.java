package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoCommentRepository extends JpaRepository<MemoComment, UUID> {

    List<MemoComment> findByMemoIdOrderByCreatedAtDesc(UUID memoId);

    List<MemoComment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    // Top-level comments only (no parent = root comments)
    List<MemoComment> findByMemoIdAndParentCommentIsNullOrderByCreatedAtDesc(UUID memoId);

    // Replies for a specific parent comment
    List<MemoComment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);
}
