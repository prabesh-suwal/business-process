package com.enterprise.memo.repository;

import com.enterprise.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoRepository extends JpaRepository<Memo, UUID> {
    List<Memo> findByCreatedBy(UUID userId);

    // Helper to count memos for a topic in the current year for numbering sequence
    long countByTopicId(UUID topicId);
}
