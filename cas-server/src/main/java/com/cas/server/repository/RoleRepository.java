package com.cas.server.repository;

import com.cas.server.domain.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByProductId(UUID productId);

    Optional<Role> findByProductIdAndCode(UUID productId, String code);

    @Query("SELECT r FROM Role r WHERE r.product.code = :productCode")
    List<Role> findByProductCode(String productCode);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(UUID id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.product.code = :productCode AND r.code = :code")
    Optional<Role> findByProductCodeAndCodeWithPermissions(String productCode, String code);

    boolean existsByProductIdAndCode(UUID productId, String code);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions LEFT JOIN FETCH r.parentRole WHERE r.product.code = :productCode")
    List<Role> findByProductCodeWithPermissionsAndParent(String productCode);
}
