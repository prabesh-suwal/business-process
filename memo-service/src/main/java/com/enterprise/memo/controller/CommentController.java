package com.enterprise.memo.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.memo.dto.CommentDTO;
import com.enterprise.memo.dto.CreateCommentRequest;
import com.enterprise.memo.service.MemoCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for memo comments.
 * Supports threaded replies and @mentions.
 */
@RestController
@RequestMapping("/api/memos/{memoId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final MemoCommentService commentService;

    /**
     * Get all comments for a memo (threaded tree structure).
     */
    @GetMapping
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable UUID memoId) {
        return ResponseEntity.ok(commentService.getCommentsForMemo(memoId));
    }

    /**
     * Add a new comment or reply to a memo.
     */
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable UUID memoId,
            @Valid @RequestBody CreateCommentRequest request) {
        UserContext user = UserContextHolder.require();
        UUID userId = UUID.fromString(user.getUserId());
        String userName = user.getName() != null ? user.getName() : "Anonymous User";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(memoId, request, userId, userName));
    }

    /**
     * Delete a comment (only by the author).
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID memoId,
            @PathVariable UUID commentId) {
        UserContext user = UserContextHolder.require();
        UUID userId = UUID.fromString(user.getUserId());

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
