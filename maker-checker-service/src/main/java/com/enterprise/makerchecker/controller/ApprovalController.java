package com.enterprise.makerchecker.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.makerchecker.dto.ApprovalActionRequest;
import com.enterprise.makerchecker.dto.ApprovalRequestResponse;
import com.enterprise.makerchecker.enums.ApprovalStatus;
import com.enterprise.makerchecker.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/maker-checker/approval-requests")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    public Page<ApprovalRequestResponse> getApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String makerUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        return approvalService.getApprovals(status, makerUserId, pageable);
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse getApproval(@PathVariable UUID id) {
        return approvalService.getApproval(id);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest action) {
        UserContext ctx = UserContextHolder.getContext();
        ApprovalRequestResponse response = approvalService.approve(
                id, action, ctx.getUserId(), ctx.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest action) {
        UserContext ctx = UserContextHolder.getContext();
        ApprovalRequestResponse response = approvalService.reject(
                id, action, ctx.getUserId(), ctx.getName());
        return ResponseEntity.ok(response);
    }
}
