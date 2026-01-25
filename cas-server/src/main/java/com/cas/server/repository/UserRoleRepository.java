package com.cas.server.repository;

import com.cas.server.domain.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId")
    void deleteAllByUserId(UUID userId);

    default void deleteByUserId(UUID userId) {
        deleteAllByUserId(userId);
    }

    /**
     * Find all user roles by role name.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.role.name = :roleName")
    List<UserRole> findByRoleName(String roleName);

    /**
     * Find all user roles for a specific role, used for authority lookup.
     * Returns all assignments for given role - filtering by maxApprovalAmount
     * must be done in service layer due to JSONB complexity.
     */
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user WHERE ur.role.name = :roleName AND ur.user.status = 'ACTIVE'")
    List<UserRole> findActiveUserRolesByRoleName(String roleName);
}
