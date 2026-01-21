package com.cas.server.domain.user;

import com.cas.server.domain.role.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "mfa_enabled")
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(length = 100)
    private String department;

    @Column(name = "user_group", length = 100)
    private String userGroup;

    // Organization Service References
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "external_provider", length = 50)
    private String externalProvider;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public enum UserStatus {
        ACTIVE, LOCKED, PENDING
    }

    /**
     * Get all roles for the user.
     */
    public Set<Role> getRoles() {
        return userRoles.stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
    }

    /**
     * Get roles for a specific product.
     */
    public Set<Role> getRolesForProduct(String productCode) {
        return userRoles.stream()
                .map(UserRole::getRole)
                .filter(role -> role.getProduct().getCode().equals(productCode))
                .collect(Collectors.toSet());
    }

    /**
     * Get all permissions for a specific product (including inherited).
     */
    public Set<String> getPermissionsForProduct(String productCode) {
        return getRolesForProduct(productCode).stream()
                .flatMap(role -> role.getAllPermissionCodes().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Get role names for a specific product.
     */
    public List<String> getRoleNamesForProduct(String productCode) {
        return getRolesForProduct(productCode).stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    /**
     * Get user role assignments for a product (includes constraints).
     */
    public Set<UserRole> getUserRolesForProduct(String productCode) {
        return userRoles.stream()
                .filter(ur -> ur.getRole().getProduct().getCode().equals(productCode))
                .collect(Collectors.toSet());
    }

    /**
     * Get aggregated constraints from all role assignments for a product.
     * Merges branchIds, regionIds and takes the max of maxApprovalAmount.
     */
    public Map<String, Object> getConstraintsForProduct(String productCode) {
        Map<String, Object> merged = new HashMap<>();
        Set<String> branchIds = new HashSet<>();
        Set<String> regionIds = new HashSet<>();
        Number maxApproval = null;

        for (UserRole ur : getUserRolesForProduct(productCode)) {
            if (ur.getConstraints() == null)
                continue;

            List<String> branches = ur.getBranchIds();
            if (branches != null)
                branchIds.addAll(branches);

            List<String> regions = ur.getRegionIds();
            if (regions != null)
                regionIds.addAll(regions);

            Number amount = ur.getMaxApprovalAmount();
            if (amount != null) {
                if (maxApproval == null || amount.doubleValue() > maxApproval.doubleValue()) {
                    maxApproval = amount;
                }
            }
        }

        if (!branchIds.isEmpty())
            merged.put("branchIds", new ArrayList<>(branchIds));
        if (!regionIds.isEmpty())
            merged.put("regionIds", new ArrayList<>(regionIds));
        if (maxApproval != null)
            merged.put("maxApprovalAmount", maxApproval);

        return merged;
    }

    /**
     * Get display name.
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return username;
        }
    }

    /**
     * Check if user has a specific permission for a product.
     */
    public boolean hasPermission(String productCode, String permissionCode) {
        return getPermissionsForProduct(productCode).contains(permissionCode);
    }
}
