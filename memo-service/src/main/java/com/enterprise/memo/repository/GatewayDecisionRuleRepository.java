package com.enterprise.memo.repository;

import com.enterprise.memo.entity.GatewayDecisionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GatewayDecisionRuleRepository extends JpaRepository<GatewayDecisionRule, UUID> {

    List<GatewayDecisionRule> findByMemoTopicIdOrderByGatewayKey(UUID memoTopicId);

    Optional<GatewayDecisionRule> findByMemoTopicIdAndGatewayKeyAndActiveTrue(UUID memoTopicId, String gatewayKey);

    List<GatewayDecisionRule> findByMemoTopicIdAndGatewayKeyOrderByVersionDesc(
            UUID memoTopicId, String gatewayKey);
}
