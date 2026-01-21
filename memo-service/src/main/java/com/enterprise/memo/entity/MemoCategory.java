package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memo_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // FINANCE, HR

    @Column(nullable = false)
    private String name; // Finance Department

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "access_policy")
    private String accessPolicy; // Simple policy string or ref to Policy Engine

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
