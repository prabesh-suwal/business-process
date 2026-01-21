package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memo_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "object_name", nullable = false)
    private String objectName; // MinIO Object ID/Path

    @Column(name = "size")
    private Long size;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
