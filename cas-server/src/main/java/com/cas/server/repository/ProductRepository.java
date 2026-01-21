package com.cas.server.repository;

import com.cas.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    List<Product> findByStatus(Product.ProductStatus status);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.permissions WHERE p.code = :code")
    Optional<Product> findByCodeWithPermissions(String code);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.roles WHERE p.code = :code")
    Optional<Product> findByCodeWithRoles(String code);
}
