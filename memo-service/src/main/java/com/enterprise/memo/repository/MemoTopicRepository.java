package com.enterprise.memo.repository;

import com.enterprise.memo.entity.MemoTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoTopicRepository extends JpaRepository<MemoTopic, UUID> {
    boolean existsByCode(String code);

    List<MemoTopic> findByCategoryId(UUID categoryId);
}
