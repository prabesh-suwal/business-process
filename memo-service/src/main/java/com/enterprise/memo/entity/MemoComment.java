package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Comments on memo or specific tasks.
 * Supports threaded replies via self-referencing parentComment.
 */
@Entity
@Table(name = "memo_comment", indexes = {
        @Index(name = "idx_memo_comment_memo", columnList = "memo_id"),
        @Index(name = "idx_memo_comment_task", columnList = "task_id"),
        @Index(name = "idx_memo_comment_parent", columnList = "parent_comment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private MemoTask task; // Optional - linked to specific task

    // Self-referencing for threaded replies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private MemoComment parentComment;

    @OneToMany(mappedBy = "parentComment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<MemoComment> replies = new ArrayList<>();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @Builder.Default
    private CommentType type = CommentType.COMMENT;

    // Comma-separated user IDs that were @mentioned
    @Column(name = "mentioned_user_ids")
    private String mentionedUserIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum CommentType {
        COMMENT, // General comment
        APPROVAL, // Approval note
        REJECTION, // Rejection reason
        CLARIFICATION // Query/clarification
    }
}
