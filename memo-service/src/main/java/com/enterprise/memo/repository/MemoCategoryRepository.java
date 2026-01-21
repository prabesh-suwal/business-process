package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MemoCategoryRepository extends JpaRepository<MemoCategory, UUID> {
    boolean existsByCode(String code);
}
