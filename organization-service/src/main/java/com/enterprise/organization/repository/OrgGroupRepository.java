package com.enterprise.organization.repository;

import com.enterprise.organization.entity.OrgGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgGroupRepository extends JpaRepository<OrgGroup, UUID> {
    Optional<OrgGroup> findByCode(String code);

    List<OrgGroup> findByStatusOrderByName(OrgGroup.Status status);

    List<OrgGroup> findByGroupTypeAndStatus(OrgGroup.GroupType groupType, OrgGroup.Status status);

    List<OrgGroup> findByBranchIdAndStatus(UUID branchId, OrgGroup.Status status);

    List<OrgGroup> findByDepartmentIdAndStatus(UUID departmentId, OrgGroup.Status status);
}
