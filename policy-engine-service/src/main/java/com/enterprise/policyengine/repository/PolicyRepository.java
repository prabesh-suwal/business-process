package com.enterprise.policyengine.repository;

import com.enterprise.policyengine.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

        Optional<Policy> findByName(String name);

        boolean existsByName(String name);

        @Query("""
                        SELECT DISTINCT p FROM Policy p
                        LEFT JOIN FETCH p.rules
                        LEFT JOIN FETCH p.ruleGroups
                        LEFT JOIN FETCH p.products
                        WHERE p.isActive = true
                        AND (LOWER(p.resourceType) = LOWER(:resourceType) OR p.resourceType = '*')
                        AND (LOWER(p.action) = LOWER(:action) OR p.action = '*')
                        AND (:product IS NULL OR :product MEMBER OF p.products OR '*' MEMBER OF p.products)
                        ORDER BY p.priority DESC
                        """)
        List<Policy> findActiveByResourceTypeAndAction(
                        @Param("resourceType") String resourceType,
                        @Param("action") String action,
                        @Param("product") String product);

        @Query("""
                        SELECT DISTINCT p FROM Policy p
                        LEFT JOIN FETCH p.rules
                        LEFT JOIN FETCH p.ruleGroups
                        WHERE p.id = :id
                        """)
        Optional<Policy> findByIdWithRules(@Param("id") UUID id);

        @Query("""
                        SELECT DISTINCT p FROM Policy p
                        LEFT JOIN FETCH p.rules
                        LEFT JOIN FETCH p.ruleGroups
                        WHERE p.isActive = true
                        ORDER BY p.resourceType, p.action, p.priority DESC
                        """)
        List<Policy> findAllActiveWithRules();

        List<Policy> findByResourceType(String resourceType);

        List<Policy> findByIsActiveTrue();
}
