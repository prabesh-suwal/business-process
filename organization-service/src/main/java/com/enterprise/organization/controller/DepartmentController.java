package com.enterprise.organization.controller;

import com.cas.common.policy.RequiresPolicy;
import com.enterprise.organization.dto.DepartmentDto;
import com.enterprise.organization.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@RequiresPolicy(resource = "DEPARTMENT")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/root")
    public ResponseEntity<List<DepartmentDto>> getRootDepartments() {
        return ResponseEntity.ok(departmentService.getRootDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartment(id));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<DepartmentDto>> getChildDepartments(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getChildDepartments(id));
    }

    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto.CreateRequest request) {
        return ResponseEntity.ok(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
            @PathVariable UUID id,
            @RequestBody DepartmentDto.UpdateRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }
}
