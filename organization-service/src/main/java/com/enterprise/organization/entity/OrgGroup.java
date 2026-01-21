package com.enterprise.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * OrgGroup - Dynamic user groups (teams, committees, working groups).
 */
@Entity
@Table(name = "org_groups")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 30)
    @Builder.Default
    private GroupType groupType = GroupType.TEAM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @ToString.Exclude
    private Branch branch; // null = organization-wide group

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    private Department department; // null = cross-department group

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<GroupMember> members = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum GroupType {
        TEAM, // Regular team
        COMMITTEE, // Approval/decision committees
        WORKING_GROUP, // Temporary project groups
        TASK_FORCE // Special assignments
    }

    public enum Status {
        ACTIVE, INACTIVE, DISSOLVED
    }
}
