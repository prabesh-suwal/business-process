package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.DecisionTable;
import com.enterprise.workflow.entity.DecisionTable.DecisionTableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionTableRepository extends JpaRepository<DecisionTable, UUID> {

    List<DecisionTable> findByProductId(UUID productId);

    List<DecisionTable> findByProductIdAndStatus(UUID productId, DecisionTableStatus status);

    List<DecisionTable> findByStatus(DecisionTableStatus status);

    Optional<DecisionTable> findByKeyAndStatus(String key, DecisionTableStatus status);

    Optional<DecisionTable> findByKeyAndVersion(String key, Integer version);

    List<DecisionTable> findByKeyOrderByVersionDesc(String key);

    boolean existsByKeyAndVersion(String key, Integer version);

    /**
     * Find the latest version number for a given key.
     */
    Optional<DecisionTable> findFirstByKeyOrderByVersionDesc(String key);
}
