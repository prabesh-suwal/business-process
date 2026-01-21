package com.cas.server.service;

import com.cas.server.domain.audit.AuditLog;
import com.cas.server.domain.user.User;
import com.cas.server.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logEvent(AuditLog.EventType eventType, AuditLog.ActorType actorType,
            UUID actorId, String targetType, UUID targetId,
            String productCode, String ipAddress, String userAgent,
            Map<String, Object> details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(eventType)
                    .actorType(actorType)
                    .actorId(actorId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .productCode(productCode)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: {} by {} on {}", eventType, actorId, targetId);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", eventType, e);
        }
    }

    public void logLoginSuccess(User user, String productCode, String ipAddress, String userAgent) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", user.getUsername());
        details.put("email", user.getEmail());

        logEvent(AuditLog.EventType.LOGIN_SUCCESS, AuditLog.ActorType.USER,
                user.getId(), "USER", user.getId(), productCode, ipAddress, userAgent, details);
    }

    public void logLoginFailure(String username, String ipAddress, String userAgent, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", username);
        details.put("reason", reason);

        logEvent(AuditLog.EventType.LOGIN_FAILURE, AuditLog.ActorType.SYSTEM,
                null, "USER", null, null, ipAddress, userAgent, details);
    }

    public void logLogout(User user, String productCode, String ipAddress) {
        logEvent(AuditLog.EventType.LOGOUT, AuditLog.ActorType.USER,
                user.getId(), "USER", user.getId(), productCode, ipAddress, null, null);
    }

    public void logRoleAssigned(UUID userId, UUID roleId, UUID assignedBy) {
        Map<String, Object> details = new HashMap<>();
        details.put("roleId", roleId.toString());

        logEvent(AuditLog.EventType.ROLE_ASSIGNED, AuditLog.ActorType.USER,
                assignedBy, "USER", userId, null, null, null, details);
    }

    public void logRoleUnassigned(UUID userId, UUID roleId, UUID removedBy) {
        Map<String, Object> details = new HashMap<>();
        details.put("roleId", roleId.toString());

        logEvent(AuditLog.EventType.ROLE_UNASSIGNED, AuditLog.ActorType.USER,
                removedBy, "USER", userId, null, null, null, details);
    }

    public void logClientSecretRotated(UUID clientId, UUID rotatedBy) {
        logEvent(AuditLog.EventType.CLIENT_SECRET_ROTATED, AuditLog.ActorType.USER,
                rotatedBy, "API_CLIENT", clientId, null, null, null, null);
    }
}
