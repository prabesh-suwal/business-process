package com.cas.server.domain.role;

import com.cas.server.domain.product.Permission;
import com.cas.server.domain.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "code" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system_role")
    @Builder.Default
    private Boolean isSystemRole = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;

    @OneToMany(mappedBy = "parentRole", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Role> childRoles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Collects all permissions including inherited ones from parent roles.
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(permissions);
        if (parentRole != null) {
            allPermissions.addAll(parentRole.getAllPermissions());
        }
        return allPermissions;
    }

    /**
     * Collects all permission codes including inherited ones.
     */
    public Set<String> getAllPermissionCodes() {
        Set<String> codes = new HashSet<>();
        for (Permission permission : getAllPermissions()) {
            codes.add(permission.getCode());
        }
        return codes;
    }
}
