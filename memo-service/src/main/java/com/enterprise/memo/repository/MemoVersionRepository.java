package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoVersionRepository extends JpaRepository<MemoVersion, UUID> {

    List<MemoVersion> findByMemoIdOrderByVersionDesc(UUID memoId);

    Integer countByMemoId(UUID memoId);
}
