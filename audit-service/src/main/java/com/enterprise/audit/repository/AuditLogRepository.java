package com.enterprise.audit.repository;

import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import com.enterprise.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>,
                JpaSpecificationExecutor<AuditLogEntity> {

        /**
         * Find the last audit log to get the previous hash for chain
         */
        Optional<AuditLogEntity> findTopByOrderBySequenceNumberDesc();

        /**
         * Get the maximum sequence number for the next entry
         */
        @Query("SELECT COALESCE(MAX(a.sequenceNumber), 0) FROM AuditLogEntity a")
        Long getMaxSequenceNumber();

        /**
         * Find by correlation ID for tracing related events across services
         */
        List<AuditLogEntity> findByCorrelationIdOrderByTimestampAsc(String correlationId);

        /**
         * Find by actor ID for audit trail
         */
        Page<AuditLogEntity> findByActorIdOrderByTimestampDesc(String actorId, Pageable pageable);

        /**
         * Find by resource for resource audit trail
         */
        Page<AuditLogEntity> findByResourceTypeAndResourceIdOrderByTimestampDesc(
                        String resourceType, String resourceId, Pageable pageable);

        /**
         * Find by category
         */
        Page<AuditLogEntity> findByCategoryOrderByTimestampDesc(AuditCategory category, Pageable pageable);

        /**
         * Find by action
         */
        Page<AuditLogEntity> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);

        /**
         * Find by service name
         */
        Page<AuditLogEntity> findByServiceNameOrderByTimestampDesc(String serviceName, Pageable pageable);

        /**
         * Find by product code
         */
        Page<AuditLogEntity> findByProductCodeOrderByTimestampDesc(String productCode, Pageable pageable);

        /**
         * Find by timestamp range
         */
        Page<AuditLogEntity> findByTimestampBetweenOrderByTimestampDesc(
                        Instant startTime, Instant endTime, Pageable pageable);

        /**
         * Complex search query
         */
        @Query(value = """
                        SELECT a FROM AuditLogEntity a
                        WHERE (:actorId IS NULL OR a.actorId = :actorId)
                        AND (:category IS NULL OR a.category = :category)
                        AND (:action IS NULL OR a.action = :action)
                        AND (:resourceType IS NULL OR a.resourceType = :resourceType)
                        AND (:resourceId IS NULL OR a.resourceId = :resourceId)
                        AND (:serviceName IS NULL OR a.serviceName = :serviceName)
                        AND (:productCode IS NULL OR a.productCode = :productCode)
                        AND (:correlationId IS NULL OR a.correlationId = :correlationId)
                        AND (:startTime IS NULL OR a.timestamp >= :startTime)
                        AND (:endTime IS NULL OR a.timestamp <= :endTime)
                        """, countQuery = """
                        SELECT COUNT(a) FROM AuditLogEntity a
                        WHERE (:actorId IS NULL OR a.actorId = :actorId)
                        AND (:category IS NULL OR a.category = :category)
                        AND (:action IS NULL OR a.action = :action)
                        AND (:resourceType IS NULL OR a.resourceType = :resourceType)
                        AND (:resourceId IS NULL OR a.resourceId = :resourceId)
                        AND (:serviceName IS NULL OR a.serviceName = :serviceName)
                        AND (:productCode IS NULL OR a.productCode = :productCode)
                        AND (:correlationId IS NULL OR a.correlationId = :correlationId)
                        AND (:startTime IS NULL OR a.timestamp >= :startTime)
                        AND (:endTime IS NULL OR a.timestamp <= :endTime)
                        """)
        Page<AuditLogEntity> searchAuditLogs(
                        @Param("actorId") String actorId,
                        @Param("category") AuditCategory category,
                        @Param("action") AuditAction action,
                        @Param("resourceType") String resourceType,
                        @Param("resourceId") String resourceId,
                        @Param("serviceName") String serviceName,
                        @Param("productCode") String productCode,
                        @Param("correlationId") String correlationId,
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime,
                        Pageable pageable);

        /**
         * Get audit logs in sequence order for chain verification
         */
        @Query("SELECT a FROM AuditLogEntity a WHERE a.sequenceNumber BETWEEN :startSeq AND :endSeq ORDER BY a.sequenceNumber ASC")
        List<AuditLogEntity> findBySequenceNumberRange(
                        @Param("startSeq") Long startSequence,
                        @Param("endSeq") Long endSequence);
}
