package com.enterprise.memo.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.memo.dto.CreateMemoRequest;
import com.enterprise.memo.dto.MemoDTO;
import com.enterprise.memo.dto.UpdateMemoRequest;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.service.MemoAccessService;
import com.enterprise.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Memos", description = "Endpoints for managing memos and their workflows")
@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;
    private final MemoAccessService memoAccessService;
    private final com.enterprise.memo.client.WorkflowClient workflowClient;

    @Operation(summary = "Create Memo Draft", description = "Creates a new memo draft for the authenticated user")
    @PostMapping("/draft")
    @com.cas.common.dto.ApiMessage("Memo draft created successfully")
    public ResponseEntity<MemoDTO> createDraft(@Valid @RequestBody CreateMemoRequest request) {
        UserContext user = UserContextHolder.require();
        UUID userUuid = UUID.fromString(user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memoService.createDraft(request, userUuid));
    }

    @Operation(summary = "Get Memo", description = "Retrieves a specific memo by ID")
    @GetMapping("/{id}")
    @com.cas.common.dto.ApiMessage("Memo retrieved successfully")
    public ResponseEntity<MemoDTO> getMemo(@PathVariable UUID id) {
        return ResponseEntity.ok(memoService.getMemo(id));
    }

    @Operation(summary = "Update Memo", description = "Updates an existing memo")
    @PutMapping("/{id}")
    @com.cas.common.dto.ApiMessage("Memo updated successfully")
    public ResponseEntity<MemoDTO> updateMemo(
            @PathVariable UUID id,
            @RequestBody UpdateMemoRequest request) {
        return ResponseEntity.ok(memoService.updateMemo(id, request));
    }

    @Operation(summary = "Get My Memos", description = "Retrieves a list of memos owned by the authenticated user")
    @GetMapping("/my-memos")
    @com.cas.common.dto.ApiMessage("My memos retrieved successfully")
    public ResponseEntity<List<MemoDTO>> getMyMemos() {
        UserContext user = UserContextHolder.require();
        UUID userUuid = UUID.fromString(user.getUserId());
        return ResponseEntity.ok(memoService.getMyMemos(userUuid));
    }

    @Operation(summary = "Submit Memo", description = "Submits a memo for workflow approval")
    @PostMapping("/{id}/submit")
    @com.cas.common.dto.ApiMessage("Memo submitted for approval")
    public ResponseEntity<MemoDTO> submitMemo(@PathVariable UUID id) {
        UserContext user = UserContextHolder.require();
        UUID userUuid = UUID.fromString(user.getUserId());
        return ResponseEntity.ok(memoService.submitMemo(id, userUuid));
    }

    // Callback endpoint for Workflow Engine
    @Operation(summary = "Update Memo Status", description = "Callback endpoint for Workflow Engine to update status")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        memoService.updateMemoStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all memos the user can access (created, involved in workflow, or viewer).
     * This is the main endpoint for the Memos page.
     */
    @Operation(summary = "Get Accessible Memos", description = "Retrieves all memos the user has access to (creator, involved, or viewer)")
    @GetMapping("/accessible")
    public ResponseEntity<List<Map<String, Object>>> getAccessibleMemos() {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        List<String> roleList = new ArrayList<>(user.getRoles());
        String departmentId = user.getDepartmentId();

        List<MemoAccessService.MemoWithAccess> memosWithAccess = memoAccessService.getAccessibleMemosWithReason(userId,
                roleList, departmentId);

        // Convert to response format
        List<Map<String, Object>> response = memosWithAccess.stream().map(mwa -> {
            Memo memo = mwa.memo();
            Map<String, Object> map = new HashMap<>();
            map.put("id", memo.getId());
            map.put("memoNumber", memo.getMemoNumber());
            map.put("subject", memo.getSubject());
            map.put("status", memo.getStatus());
            map.put("currentStage", memo.getCurrentStage());
            map.put("topicId", memo.getTopic() != null ? memo.getTopic().getId() : null);
            map.put("topicName", memo.getTopic() != null ? memo.getTopic().getName() : null);
            map.put("categoryName", memo.getTopic() != null && memo.getTopic().getCategory() != null
                    ? memo.getTopic().getCategory().getName()
                    : null);
            map.put("createdBy", memo.getCreatedBy());
            map.put("createdAt", memo.getCreatedAt());
            map.put("updatedAt", memo.getUpdatedAt());
            map.put("accessReason", mwa.accessReason().name());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get memos that user can view (including view-only access).
     */
    @Operation(summary = "Get Viewable Memos", description = "Retrieves memos that user can view (including view-only access)")
    @GetMapping("/viewable")
    public ResponseEntity<List<MemoDTO>> getViewableMemos() {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        List<String> roleList = new ArrayList<>(user.getRoles());
        String departmentId = user.getDepartmentId();

        return ResponseEntity.ok(memoService.getViewableMemos(userId, roleList, departmentId));
    }

    /**
     * Check if user can view a specific memo.
     */
    @Operation(summary = "Check Memo Visibility", description = "Checks if user has permission to view a specific memo")
    @GetMapping("/{id}/can-view")
    public ResponseEntity<Boolean> canViewMemo(@PathVariable UUID id) {
        UserContext user = UserContextHolder.require();
        String userId = user.getUserId();
        List<String> roleList = new ArrayList<>(user.getRoles());
        String departmentId = user.getDepartmentId();

        boolean canView = memoService.canViewMemo(id, userId, roleList, departmentId);
        return ResponseEntity.ok(canView);
    }

    /**
     * Get execution history (timeline) for a memo's workflow.
     * Proxies to workflow-service via WorkflowClient.
     */
    @Operation(summary = "Get Memo History", description = "Retrieves execution history (timeline) for a memo's workflow")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@PathVariable UUID id) {
        MemoDTO memo = memoService.getMemo(id);
        if (memo.getProcessInstanceId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(workflowClient.getTimeline(memo.getProcessInstanceId()));
    }
}
