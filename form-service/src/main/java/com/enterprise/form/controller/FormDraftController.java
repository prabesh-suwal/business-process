package com.enterprise.form.controller;

import com.enterprise.form.dto.FormDraftDTO;
import com.enterprise.form.dto.SaveDraftRequest;
import com.enterprise.form.service.FormDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for form draft operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/forms/drafts")
@RequiredArgsConstructor
public class FormDraftController {

    private final FormDraftService draftService;

    /**
     * Save a draft.
     */
    @PostMapping
    public ResponseEntity<FormDraftDTO> saveDraft(
            @RequestBody SaveDraftRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        UUID uid = userId != null ? UUID.fromString(userId) : null;
        if (uid == null) {
            return ResponseEntity.badRequest().build();
        }

        FormDraftDTO result = draftService.saveDraft(request, uid, userName);
        return ResponseEntity.ok(result);
    }

    /**
     * Get user's draft for a form.
     */
    @GetMapping("/form/{formId}")
    public ResponseEntity<FormDraftDTO> getDraft(
            @PathVariable UUID formId,
            @RequestHeader(value = "X-User-Id") String userId) {

        UUID uid = UUID.fromString(userId);
        return draftService.getDraft(formId, uid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user's draft for a form linked to entity.
     */
    @GetMapping("/form/{formId}/entity/{entityType}/{entityId}")
    public ResponseEntity<FormDraftDTO> getDraftByEntity(
            @PathVariable UUID formId,
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestHeader(value = "X-User-Id") String userId) {

        UUID uid = UUID.fromString(userId);
        return draftService.getDraft(formId, uid, entityType, entityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all user's drafts.
     */
    @GetMapping("/my-drafts")
    public ResponseEntity<List<FormDraftDTO>> getMyDrafts(
            @RequestHeader(value = "X-User-Id") String userId) {

        UUID uid = UUID.fromString(userId);
        return ResponseEntity.ok(draftService.getUserDrafts(uid));
    }

    /**
     * Delete a draft.
     */
    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable UUID draftId,
            @RequestHeader(value = "X-User-Id") String userId) {

        try {
            UUID uid = UUID.fromString(userId);
            draftService.deleteDraft(draftId, uid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
