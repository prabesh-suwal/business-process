package com.cas.server.repository;

import com.cas.server.domain.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByProductCode(String productCode);

    @Query("SELECT p FROM Policy p WHERE p.productCode IS NULL")
    List<Policy> findGlobalPolicies();

    @Query("SELECT p FROM Policy p WHERE (p.productCode IS NULL OR p.productCode = :productCode) AND p.status = 'ACTIVE' ORDER BY p.priority DESC")
    List<Policy> findActivePoliciesForProduct(String productCode);

    List<Policy> findByPolicyType(Policy.PolicyType policyType);

    List<Policy> findByStatus(Policy.PolicyStatus status);
}
