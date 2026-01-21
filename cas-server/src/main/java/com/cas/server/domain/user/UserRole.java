package com.cas.server.domain.user;

import com.cas.server.domain.role.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "role_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * ABAC constraints for this role assignment.
     * Limits where/how this role's permissions apply.
     * 
     * Example: {"branchIds": ["KTM-001"], "maxApprovalAmount": 500000}
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> constraints = Map.of();

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    // Helper methods for common constraints
    @SuppressWarnings("unchecked")
    public List<String> getBranchIds() {
        return constraints != null ? (List<String>) constraints.get("branchIds") : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRegionIds() {
        return constraints != null ? (List<String>) constraints.get("regionIds") : null;
    }

    public Number getMaxApprovalAmount() {
        return constraints != null ? (Number) constraints.get("maxApprovalAmount") : null;
    }
}
