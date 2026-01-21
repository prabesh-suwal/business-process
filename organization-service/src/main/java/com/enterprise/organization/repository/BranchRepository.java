package com.enterprise.organization.repository;

import com.enterprise.organization.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findByCode(String code);

    List<Branch> findByStatusOrderByName(Branch.Status status);

    List<Branch> findByGeoLocationIdAndStatus(UUID geoLocationId, Branch.Status status);

    List<Branch> findByParentBranchIdAndStatus(UUID parentBranchId, Branch.Status status);

    List<Branch> findByBranchTypeAndStatus(Branch.BranchType branchType, Branch.Status status);
}
