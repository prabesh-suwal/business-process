package com.cas.server.domain.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum EventType {
        // Authentication events
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        PASSWORD_CHANGE,
        MFA_ENABLED,
        MFA_DISABLED,

        // Token events
        TOKEN_ISSUED,
        TOKEN_REFRESHED,
        TOKEN_REVOKED,

        // User management
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_LOCKED,
        USER_UNLOCKED,

        // Role management
        ROLE_CREATED,
        ROLE_UPDATED,
        ROLE_DELETED,
        ROLE_ASSIGNED,
        ROLE_UNASSIGNED,
        PERMISSION_ASSIGNED,
        PERMISSION_UNASSIGNED,

        // Client management
        CLIENT_CREATED,
        CLIENT_UPDATED,
        CLIENT_DELETED,
        CLIENT_SECRET_ROTATED,

        // API access
        API_ACCESS,
        API_ACCESS_DENIED,

        // Policy events
        POLICY_CREATED,
        POLICY_UPDATED,
        POLICY_DELETED
    }

    public enum ActorType {
        USER,
        CLIENT,
        SYSTEM
    }
}
