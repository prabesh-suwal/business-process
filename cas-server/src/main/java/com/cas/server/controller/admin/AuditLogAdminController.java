package com.cas.server.controller.admin;

import com.cas.server.domain.audit.AuditLog;
import com.cas.server.dto.AuditLogDto;
import com.cas.server.dto.PageDto;
import com.cas.server.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogAdminController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<PageDto<AuditLogDto>> listAuditLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Sort sort = Sort.by("createdAt").descending();
        Page<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(page, size, sort));

        PageDto<AuditLogDto> result = PageDto.<AuditLogDto>builder()
                .content(logs.getContent().stream().map(this::toDto).toList())
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .first(logs.isFirst())
                .last(logs.isLast())
                .build();

        return ResponseEntity.ok(result);
    }

    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .eventType(log.getEventType().name())
                .actorType(log.getActorType().name())
                .actorId(log.getActorId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .productCode(log.getProductCode())
                .ipAddress(log.getIpAddress())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
