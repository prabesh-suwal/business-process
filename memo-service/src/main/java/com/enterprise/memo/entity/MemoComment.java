package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comments on memo or specific tasks.
 */
@Entity
@Table(name = "memo_comment", indexes = {
        @Index(name = "idx_memo_comment_memo", columnList = "memo_id"),
        @Index(name = "idx_memo_comment_task", columnList = "task_id")
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

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type")
    @Builder.Default
    private CommentType type = CommentType.COMMENT;

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
