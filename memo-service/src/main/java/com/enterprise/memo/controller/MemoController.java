package com.enterprise.memo.controller;

import com.enterprise.memo.dto.CreateMemoRequest;
import com.enterprise.memo.dto.MemoDTO;
import com.enterprise.memo.dto.UpdateMemoRequest;
import com.enterprise.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping("/draft")
    public ResponseEntity<MemoDTO> createDraft(
            @Valid @RequestBody CreateMemoRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId) {
        // In real gateway, X-User-Id is populated. Using default for dev if missing.
        UUID userUuid = UUID.fromString(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memoService.createDraft(request, userUuid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoDTO> getMemo(@PathVariable UUID id) {
        return ResponseEntity.ok(memoService.getMemo(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoDTO> updateMemo(
            @PathVariable UUID id,
            @RequestBody UpdateMemoRequest request) {
        return ResponseEntity.ok(memoService.updateMemo(id, request));
    }

    @GetMapping("/my-memos")
    public ResponseEntity<List<MemoDTO>> getMyMemos(
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId) {
        UUID userUuid = UUID.fromString(userId);
        return ResponseEntity.ok(memoService.getMyMemos(userUuid));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<MemoDTO> submitMemo(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000000") String userId) {
        UUID userUuid = UUID.fromString(userId);
        return ResponseEntity.ok(memoService.submitMemo(id, userUuid));
    }

    // Callback endpoint for Workflow Engine
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        memoService.updateMemoStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
