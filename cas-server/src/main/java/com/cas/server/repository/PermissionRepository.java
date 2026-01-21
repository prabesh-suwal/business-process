package com.cas.server.repository;

import com.cas.server.domain.product.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query("SELECT p FROM Permission p JOIN p.products prod WHERE prod.id = :productId")
    List<Permission> findByProductId(UUID productId);

    @Query("SELECT p FROM Permission p JOIN p.products prod WHERE prod.id = :productId AND p.code = :code")
    Optional<Permission> findByProductIdAndCode(UUID productId, String code);

    @Query("SELECT p FROM Permission p JOIN p.products prod WHERE prod.code = :productCode")
    List<Permission> findByProductCode(String productCode);

    @Query("SELECT p FROM Permission p JOIN p.products prod WHERE prod.code = :productCode AND p.code IN :codes")
    Set<Permission> findByProductCodeAndCodeIn(String productCode, Set<String> codes);

    @Query("SELECT p FROM Permission p WHERE p.id IN :ids")
    Set<Permission> findByIdIn(Set<UUID> ids);

    Optional<Permission> findByCode(String code);
}
