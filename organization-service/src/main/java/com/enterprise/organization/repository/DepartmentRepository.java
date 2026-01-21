package com.enterprise.organization.repository;

import com.enterprise.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByCode(String code);

    List<Department> findByStatusOrderByName(Department.Status status);

    List<Department> findByParentIdAndStatus(UUID parentId, Department.Status status);

    List<Department> findByBranchIdAndStatus(UUID branchId, Department.Status status);

    List<Department> findByParentIsNullAndStatus(Department.Status status);
}
