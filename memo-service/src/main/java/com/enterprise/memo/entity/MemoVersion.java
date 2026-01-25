package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of memo data for audit trail.
 * Created on significant events (submit, approve, reject).
 */
@Entity
@Table(name = "memo_version", indexes = {
        @Index(name = "idx_memo_version_memo", columnList = "memo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @Column(nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> dataSnapshot;

    @Column(name = "status_at_snapshot")
    private String statusAtSnapshot;

    @Column(name = "stage_at_snapshot")
    private String stageAtSnapshot;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "action")
    private String action; // SUBMIT, APPROVE, REJECT, EDIT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
