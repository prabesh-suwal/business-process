package com.enterprise.lms.repository;

import com.enterprise.lms.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {

    Optional<LoanProduct> findByCode(String code);

    List<LoanProduct> findByActiveTrue();

    List<LoanProduct> findByProductId(UUID productId);

    boolean existsByCode(String code);
}
