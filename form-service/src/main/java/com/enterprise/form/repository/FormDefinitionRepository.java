package com.enterprise.form.repository;

import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.entity.FormDefinition.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {

        List<FormDefinition> findByProductIdOrderByNameAsc(UUID productId);

        List<FormDefinition> findByProductIdAndStatusOrderByNameAsc(
                        UUID productId, FormDefinition.FormStatus status);

        Optional<FormDefinition> findByProductIdAndNameAndVersion(
                        UUID productId, String name, Integer version);

        @Query("SELECT MAX(f.version) FROM FormDefinition f WHERE f.productId = :productId AND f.name = :name")
        Optional<Integer> findMaxVersionByProductIdAndName(
                        @Param("productId") UUID productId, @Param("name") String name);

        boolean existsByName(String name);

        // === VERSIONING QUERIES ===

        /**
         * Find forms by product, name, and status ordered by version desc.
         */
        List<FormDefinition> findByProductIdAndNameAndStatusOrderByVersionDesc(
                        UUID productId, String name, FormStatus status);

        /**
         * Find all versions of a form.
         */
        List<FormDefinition> findByProductIdAndNameOrderByVersionDesc(UUID productId, String name);

        /**
         * Find all active forms for a product.
         */
        List<FormDefinition> findByProductIdAndStatusOrderByNameAscVersionDesc(
                        UUID productId, FormStatus status);
}
