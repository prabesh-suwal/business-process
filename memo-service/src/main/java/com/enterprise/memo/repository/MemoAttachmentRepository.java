package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoAttachmentRepository extends JpaRepository<MemoAttachment, UUID> {
    List<MemoAttachment> findByMemoId(UUID memoId);
}
