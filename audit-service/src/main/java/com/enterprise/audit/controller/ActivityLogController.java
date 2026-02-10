package com.enterprise.audit.controller;

import com.cas.common.logging.activity.ActivityLogEvent;
import com.enterprise.audit.entity.ActivityLogEntity;
import com.enterprise.audit.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Controller for Activity logs (user timeline).
 * Handles ingestion and querying of user activity logs.
 */
@RestController
@RequestMapping("/api/logs/activity")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Ingest a new activity log event.
     */
    @PostMapping
    public ResponseEntity<Void> ingest(@RequestBody ActivityLogEvent event) {
        log.trace("Ingesting activity log: {}", event.getActivityId());

        ActivityLogEntity entity = ActivityLogEntity.builder()
                .activityId(UUID.fromString(event.getActivityId()))
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .correlationId(event.getCorrelationId())
                .userId(event.getUserId())
                .username(event.getUsername())
                .userRole(event.getUserRole())
                .activityType(event.getActivityType() != null ? event.getActivityType().name() : null)
                .moduleName(event.getModuleName())
                .entityName(event.getEntityName())
                .entityId(event.getEntityId())
                .description(event.getDescription())
                .status(event.getStatus())
                .ipAddress(event.getIpAddress())
                .deviceInfo(event.getDeviceInfo())
                .geoLocation(event.getGeoLocation())
                .build();

        activityLogRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    /**
     * Search activity logs with filters.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ActivityLogEntity>> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ActivityLogEntity> logs = activityLogRepository.searchLogs(
                userId, activityType, moduleName, entityName, status, startTime, endTime, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get activity logs for a specific user (user timeline).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ActivityLogEntity>> getByUser(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ActivityLogEntity> logs = activityLogRepository.findByUserId(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get a specific activity log by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityLogEntity> getById(@PathVariable UUID id) {
        return activityLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
