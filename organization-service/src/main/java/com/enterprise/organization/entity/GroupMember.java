package com.enterprise.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * GroupMember - Links users to groups with role information.
 */
@Entity
@Table(name = "group_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group_id", "user_id" })
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrgGroup group;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // References CAS user

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 30)
    @Builder.Default
    private MemberRole memberRole = MemberRole.MEMBER;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    public enum MemberRole {
        LEADER, // Group leader/head
        SECRETARY, // Secretary
        MEMBER // Regular member
    }
}
