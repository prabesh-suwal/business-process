package com.enterprise.audit.repository;

import com.enterprise.audit.entity.ApiLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLogEntity, UUID> {

    Page<ApiLogEntity> findByServiceName(String serviceName, Pageable pageable);

    Page<ApiLogEntity> findByCorrelationId(String correlationId, Pageable pageable);

    Page<ApiLogEntity> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    @Query("SELECT a FROM ApiLogEntity a WHERE " +
            "(:serviceName IS NULL OR a.serviceName = :serviceName) AND " +
            "(:correlationId IS NULL OR a.correlationId = :correlationId) AND " +
            "(:httpMethod IS NULL OR a.httpMethod = :httpMethod) AND " +
            "(:responseStatus IS NULL OR a.responseStatus = :responseStatus) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp <= :endTime)")
    Page<ApiLogEntity> searchLogs(
            @Param("serviceName") String serviceName,
            @Param("correlationId") String correlationId,
            @Param("httpMethod") String httpMethod,
            @Param("responseStatus") Integer responseStatus,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
