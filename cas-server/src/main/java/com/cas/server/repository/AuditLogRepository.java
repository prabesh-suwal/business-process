package com.cas.server.repository;

import com.cas.server.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);

    Page<AuditLog> findByEventType(AuditLog.EventType eventType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(Instant start, Instant end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.productCode = :productCode ORDER BY a.createdAt DESC")
    Page<AuditLog> findByProductCode(String productCode, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.targetType = :targetType AND a.targetId = :targetId ORDER BY a.createdAt DESC")
    List<AuditLog> findByTarget(String targetType, UUID targetId);
}
