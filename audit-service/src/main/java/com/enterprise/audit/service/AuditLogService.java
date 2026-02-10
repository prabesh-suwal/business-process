package com.enterprise.audit.service;

import com.cas.common.audit.AuditEvent;
import com.enterprise.audit.dto.AuditLogResponse;
import com.enterprise.audit.dto.AuditSearchRequest;
import com.enterprise.audit.dto.IntegrityCheckResult;
import com.enterprise.audit.entity.AuditLogEntity;
import com.enterprise.audit.mapper.AuditLogMapper;
import com.enterprise.audit.repository.AuditLogRepository;
import com.enterprise.audit.repository.AuditLogSpecifications;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * Persists an audit event with hash chain integrity.
     * This is the main entry point for receiving audit events from other services.
     */
    @Transactional
    public AuditLogResponse createAuditLog(AuditEvent event) {
        log.debug("Creating audit log for action: {} on resource: {}/{}",
                event.getAction(), event.getResourceType(), event.getResourceId());

        // Get the previous hash for chain integrity
        Optional<AuditLogEntity> lastEntry = repository.findTopByOrderBySequenceNumberDesc();
        String previousHash = lastEntry.map(AuditLogEntity::getRecordHash).orElse("GENESIS");

        // Get next sequence number
        Long nextSequence = repository.getMaxSequenceNumber() + 1;

        // Convert metadata map to JSON string
        String metadataJson = serializeMetadata(event.getMetadata());

        // Convert roles list to comma-separated string
        String rolesStr = event.getActorRoles() != null
                ? String.join(",", event.getActorRoles())
                : null;

        // Build the entity
        AuditLogEntity entity = AuditLogEntity.builder()
                .sequenceNumber(nextSequence)
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .actorId(event.getActorId())
                .actorName(event.getActorName())
                .actorEmail(event.getActorEmail())
                .actorType(event.getActorType())
                .actorRoles(rolesStr)
                .ipAddress(event.getIpAddress())
                .action(event.getAction())
                .category(event.getCategory())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .description(event.getDescription())
                .metadata(metadataJson)
                .result(event.getResult())
                .errorMessage(event.getFailureReason())
                .serviceName(event.getServiceName())
                .productCode(event.getProductCode())
                .correlationId(event.getCorrelationId())
                .previousHash(previousHash)
                .build();

        // Calculate and set the record hash
        String recordHash = entity.calculateContentHash();
        entity = entity.toBuilder().recordHash(recordHash).build();

        AuditLogEntity saved = repository.save(entity);
        log.info("Audit log created: id={}, seq={}, action={}",
                saved.getId(), saved.getSequenceNumber(), saved.getAction());

        return mapper.toResponse(saved);
    }

    /**
     * Retrieves an audit log by ID
     */
    @Transactional(readOnly = true)
    public Optional<AuditLogResponse> getAuditLog(UUID id) {
        return repository.findById(id).map(mapper::toResponse);
    }

    /**
     * Search audit logs with various filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(AuditSearchRequest request, Pageable pageable) {
        var spec = AuditLogSpecifications.withFilters(
                request.getActorId(),
                request.getCategory(),
                request.getAction(),
                request.getResourceType(),
                request.getResourceId(),
                request.getServiceName(),
                request.getProductCode(),
                request.getCorrelationId(),
                request.getStartTime(),
                request.getEndTime());

        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    /**
     * Get all audit logs for a correlation ID (transaction trace)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByCorrelationId(String correlationId) {
        return repository.findByCorrelationIdOrderByTimestampAsc(correlationId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Get audit trail for a specific resource
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getResourceAuditTrail(
            String resourceType, String resourceId, Pageable pageable) {
        return repository.findByResourceTypeAndResourceIdOrderByTimestampDesc(
                resourceType, resourceId, pageable).map(mapper::toResponse);
    }

    /**
     * Get audit trail for a specific actor
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getActorAuditTrail(String actorId, Pageable pageable) {
        return repository.findByActorIdOrderByTimestampDesc(actorId, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Verify integrity of audit log chain within a sequence range.
     * Returns details about any integrity violations found.
     */
    @Transactional(readOnly = true)
    public IntegrityCheckResult verifyIntegrity(Long startSequence, Long endSequence) {
        log.info("Verifying audit log integrity from seq {} to {}", startSequence, endSequence);

        List<AuditLogEntity> logs = repository.findBySequenceNumberRange(startSequence, endSequence);

        if (logs.isEmpty()) {
            return IntegrityCheckResult.builder()
                    .verified(true)
                    .checkedCount(0)
                    .message("No records found in the specified range")
                    .build();
        }

        int violations = 0;
        StringBuilder details = new StringBuilder();
        String expectedPreviousHash = startSequence == 1 ? "GENESIS" : null;

        for (int i = 0; i < logs.size(); i++) {
            AuditLogEntity log = logs.get(i);

            // Verify record's own hash
            if (!log.verifyIntegrity()) {
                violations++;
                details.append(String.format("Seq %d: Content hash mismatch - possible tampering!\n",
                        log.getSequenceNumber()));
            }

            // Verify chain link (previous hash)
            if (expectedPreviousHash != null && !expectedPreviousHash.equals(log.getPreviousHash())) {
                violations++;
                details.append(String.format("Seq %d: Chain broken - previous hash mismatch!\n",
                        log.getSequenceNumber()));
            }

            expectedPreviousHash = log.getRecordHash();
        }

        return IntegrityCheckResult.builder()
                .verified(violations == 0)
                .checkedCount(logs.size())
                .violationsCount(violations)
                .startSequence(logs.get(0).getSequenceNumber())
                .endSequence(logs.get(logs.size() - 1).getSequenceNumber())
                .message(violations == 0
                        ? "All records verified successfully"
                        : "Integrity violations detected")
                .details(details.toString())
                .build();
    }

    private String serializeMetadata(Object metadata) {
        if (metadata == null)
            return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata: {}", e.getMessage());
            return null;
        }
    }
}
