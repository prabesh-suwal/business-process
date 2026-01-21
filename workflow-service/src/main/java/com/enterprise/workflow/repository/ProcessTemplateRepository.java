package com.enterprise.workflow.repository;

import com.enterprise.workflow.entity.ProcessTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessTemplateRepository extends JpaRepository<ProcessTemplate, UUID> {

        List<ProcessTemplate> findByProductIdOrderByNameAsc(UUID productId);

        List<ProcessTemplate> findByProductIdAndStatusOrderByNameAsc(
                        UUID productId, ProcessTemplate.ProcessTemplateStatus status);

        List<ProcessTemplate> findByStatusOrderByNameAsc(ProcessTemplate.ProcessTemplateStatus status);

        Optional<ProcessTemplate> findByProductIdAndNameAndVersion(
                        UUID productId, String name, Integer version);

        Optional<ProcessTemplate> findByFlowableProcessDefKey(String processDefKey);

        @Query("SELECT MAX(pt.version) FROM ProcessTemplate pt WHERE pt.productId = :productId AND pt.name = :name")
        Optional<Integer> findMaxVersionByProductIdAndName(
                        @Param("productId") UUID productId, @Param("name") String name);

        boolean existsByProductIdAndNameAndVersion(UUID productId, String name, Integer version);

        // === VERSIONING QUERIES ===

        /**
         * Find the currently effective template by product and name.
         * Returns the ACTIVE template where now is within effective dates.
         */
        @Query("SELECT pt FROM ProcessTemplate pt WHERE pt.productId = :productId AND pt.name = :name " +
                        "AND pt.status = 'ACTIVE' " +
                        "AND (pt.effectiveFrom IS NULL OR pt.effectiveFrom <= :now) " +
                        "AND (pt.effectiveTo IS NULL OR pt.effectiveTo > :now) " +
                        "ORDER BY pt.version DESC")
        List<ProcessTemplate> findEffectiveByProductIdAndName(
                        @Param("productId") UUID productId,
                        @Param("name") String name,
                        @Param("now") LocalDateTime now);

        /**
         * Find all currently effective templates for a product.
         */
        @Query("SELECT pt FROM ProcessTemplate pt WHERE pt.productId = :productId " +
                        "AND pt.status = 'ACTIVE' " +
                        "AND (pt.effectiveFrom IS NULL OR pt.effectiveFrom <= :now) " +
                        "AND (pt.effectiveTo IS NULL OR pt.effectiveTo > :now) " +
                        "ORDER BY pt.name ASC")
        List<ProcessTemplate> findAllEffectiveByProductId(
                        @Param("productId") UUID productId,
                        @Param("now") LocalDateTime now);

        /**
         * Find all versions of a template by product and name.
         */
        List<ProcessTemplate> findByProductIdAndNameOrderByVersionDesc(UUID productId, String name);
}
