package com.enterprise.organization.service;

import com.cas.common.logging.audit.AuditEventType;
import com.cas.common.logging.audit.AuditLogger;
import com.enterprise.organization.dto.DepartmentDto;
import com.enterprise.organization.entity.Department;
import com.enterprise.organization.repository.BranchRepository;
import com.enterprise.organization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final AuditLogger auditLogger;

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findByStatusOrderByName(Department.Status.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<DepartmentDto> getRootDepartments() {
        return departmentRepository.findByParentIsNullAndStatus(Department.Status.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<DepartmentDto> getChildDepartments(UUID parentId) {
        return departmentRepository.findByParentIdAndStatus(parentId, Department.Status.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public DepartmentDto getDepartment(UUID id) {
        return departmentRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentDto.CreateRequest request) {
        Department parent = request.getParentId() != null
                ? departmentRepository.findById(request.getParentId()).orElse(null)
                : null;

        Department dept = Department.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .parent(parent)
                .branch(request.getBranchId() != null
                        ? branchRepository.findById(request.getBranchId()).orElse(null)
                        : null)
                .headUserId(request.getHeadUserId())
                .status(Department.Status.ACTIVE)
                .build();

        dept = departmentRepository.save(dept);
        log.info("Created department: {}", dept.getName());

        // Audit log with builder pattern
        auditLogger.log()
                .eventType(AuditEventType.CREATE)
                .action("Created new department")
                .module("ORGANIZATION")
                .entity("DEPARTMENT", dept.getId().toString())
                .businessKey(dept.getCode())
                .newValue(toDto(dept))
                .success();

        return toDto(dept);
    }

    @Transactional
    public DepartmentDto updateDepartment(UUID id, DepartmentDto.UpdateRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        // Capture old state for audit
        DepartmentDto oldState = toDto(dept);

        if (request.getName() != null)
            dept.setName(request.getName());
        if (request.getDescription() != null)
            dept.setDescription(request.getDescription());
        if (request.getHeadUserId() != null)
            dept.setHeadUserId(request.getHeadUserId());
        if (request.getStatus() != null)
            dept.setStatus(Department.Status.valueOf(request.getStatus()));

        dept = departmentRepository.save(dept);
        log.info("Updated department: {}", dept.getName());

        // Audit log with builder pattern
        auditLogger.log()
                .eventType(AuditEventType.UPDATE)
                .action("Updated department")
                .module("ORGANIZATION")
                .entity("DEPARTMENT", id.toString())
                .businessKey(dept.getCode())
                .oldValue(oldState)
                .newValue(toDto(dept))
                .success();

        return toDto(dept);
    }

    private DepartmentDto toDto(Department d) {
        return DepartmentDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .description(d.getDescription())
                .parentId(d.getParent() != null ? d.getParent().getId() : null)
                .parentName(d.getParent() != null ? d.getParent().getName() : null)
                .branchId(d.getBranch() != null ? d.getBranch().getId() : null)
                .branchName(d.getBranch() != null ? d.getBranch().getName() : null)
                .headUserId(d.getHeadUserId())
                .status(d.getStatus().name())
                .build();
    }
}
