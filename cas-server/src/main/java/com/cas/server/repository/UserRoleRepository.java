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
}
