package com.enterprise.memo.service;

import com.cas.common.logging.audit.AuditEventType;
import com.cas.common.logging.audit.AuditLogger;
import com.enterprise.memo.dto.CommentDTO;
import com.enterprise.memo.dto.CreateCommentRequest;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoComment;
import com.enterprise.memo.repository.MemoCommentRepository;
import com.enterprise.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing memo comments with threaded replies and @mentions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemoCommentService {

    private final MemoCommentRepository commentRepository;
    private final MemoRepository memoRepository;
    private final AuditLogger auditLogger;

    /**
     * Get all comments for a memo as a threaded tree.
     * Returns top-level comments with nested replies.
     */
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsForMemo(UUID memoId) {
        List<MemoComment> topLevelComments = commentRepository
                .findByMemoIdAndParentCommentIsNullOrderByCreatedAtDesc(memoId);

        return topLevelComments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Add a new comment or reply to a memo.
     */
    @Transactional
    public CommentDTO addComment(UUID memoId, CreateCommentRequest request, UUID userId, String userName) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        MemoComment.MemoCommentBuilder builder = MemoComment.builder()
                .memo(memo)
                .userId(userId)
                .userName(userName)
                .content(request.getContent())
                .type(MemoComment.CommentType.COMMENT);

        // Handle reply
        if (request.getParentCommentId() != null) {
            MemoComment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(
                            () -> new RuntimeException("Parent comment not found: " + request.getParentCommentId()));
            builder.parentComment(parent);
        }

        // Handle mentions
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            builder.mentionedUserIds(String.join(",", request.getMentionedUserIds()));
        }

        MemoComment comment = commentRepository.save(builder.build());
        log.info("Comment {} added to memo {} by user {}", comment.getId(), memoId, userName);

        // Audit log
        String action = request.getParentCommentId() != null
                ? "Replied to comment on memo"
                : "Added comment to memo";

        var auditBuilder = auditLogger.log()
                .eventType(AuditEventType.CREATE)
                .action(action)
                .module("COMMENT")
                .entity("MEMO_COMMENT", comment.getId().toString())
                .businessKey(memo.getMemoNumber())
                .newValue(toDTO(comment));

        if (request.getParentCommentId() != null) {
            auditBuilder.remarks("Reply to comment: " + request.getParentCommentId());
        }

        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            auditBuilder.remarks("Mentioned users: " + String.join(", ", request.getMentionedUserIds()));
        }

        auditBuilder.success();

        return toDTO(comment);
    }

    /**
     * Delete a comment (only by the comment author).
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        MemoComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Only the comment author can delete this comment");
        }

        CommentDTO deletedComment = toDTO(comment);

        // Delete the comment and its replies (cascaded)
        commentRepository.delete(comment);

        log.info("Comment {} deleted by user {}", commentId, userId);

        // Audit log
        auditLogger.log()
                .eventType(AuditEventType.DELETE)
                .action("Deleted comment from memo")
                .module("COMMENT")
                .entity("MEMO_COMMENT", commentId.toString())
                .businessKey(comment.getMemo().getMemoNumber())
                .oldValue(deletedComment)
                .success();
    }

    /**
     * Convert entity to DTO with nested replies.
     */
    private CommentDTO toDTO(MemoComment comment) {
        List<String> mentionIds = comment.getMentionedUserIds() != null && !comment.getMentionedUserIds().isBlank()
                ? Arrays.asList(comment.getMentionedUserIds().split(","))
                : Collections.emptyList();

        List<CommentDTO> replyDTOs = comment.getReplies() != null
                ? comment.getReplies().stream().map(this::toDTO).collect(Collectors.toList())
                : Collections.emptyList();

        return CommentDTO.builder()
                .id(comment.getId())
                .memoId(comment.getMemo().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .content(comment.getContent())
                .type(comment.getType().name())
                .mentionedUserIds(mentionIds)
                .createdAt(comment.getCreatedAt())
                .replies(replyDTOs)
                .build();
    }
}
