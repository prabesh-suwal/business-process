package com.enterprise.audit.repository;

import com.enterprise.audit.entity.ActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {

    Page<ActivityLogEntity> findByUserId(String userId, Pageable pageable);

    Page<ActivityLogEntity> findByActivityType(String activityType, Pageable pageable);

    Page<ActivityLogEntity> findByModuleName(String moduleName, Pageable pageable);

    Page<ActivityLogEntity> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    @Query("SELECT a FROM ActivityLogEntity a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:activityType IS NULL OR a.activityType = :activityType) AND " +
            "(:moduleName IS NULL OR a.moduleName = :moduleName) AND " +
            "(:entityName IS NULL OR a.entityName = :entityName) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp <= :endTime)")
    Page<ActivityLogEntity> searchLogs(
            @Param("userId") String userId,
            @Param("activityType") String activityType,
            @Param("moduleName") String moduleName,
            @Param("entityName") String entityName,
            @Param("status") String status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
