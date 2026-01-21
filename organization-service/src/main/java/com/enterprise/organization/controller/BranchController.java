package com.enterprise.organization.controller;

import com.cas.common.policy.RequiresPolicy;
import com.enterprise.organization.dto.BranchDto;
import com.enterprise.organization.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@RequiresPolicy(resource = "BRANCH", skip = true)
public class BranchController {

    private final BranchService branchService;

    @RequiresPolicy(resource = "BRANCH", skip = true)
    @GetMapping
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> getBranch(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getBranch(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<BranchDto> getBranchByCode(@PathVariable String code) {
        return ResponseEntity.ok(branchService.getBranchByCode(code));
    }

    @PostMapping
    public ResponseEntity<BranchDto> createBranch(@RequestBody BranchDto.CreateRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDto> updateBranch(
            @PathVariable UUID id,
            @RequestBody BranchDto.UpdateRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
