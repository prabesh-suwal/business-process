package com.cas.server.domain.client;

import com.cas.server.domain.product.Permission;
import com.cas.server.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "api_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "allowed_ips", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] allowedIps;

    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 100;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "api_client_scopes", joinColumns = @JoinColumn(name = "client_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default
    private Set<Permission> scopes = new HashSet<>();

    public enum ClientStatus {
        ACTIVE, SUSPENDED, EXPIRED
    }

    /**
     * Get all scope codes for this client.
     */
    public Set<String> getScopeCodes() {
        return scopes.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * Check if client has a specific scope.
     */
    public boolean hasScope(String scopeCode) {
        return getScopeCodes().contains(scopeCode);
    }

    /**
     * Check if client is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if client is active (not suspended, expired, or past expiry date).
     */
    public boolean isActive() {
        return status == ClientStatus.ACTIVE && !isExpired();
    }

    /**
     * Check if IP is allowed.
     */
    public boolean isIpAllowed(String ip) {
        if (allowedIps == null || allowedIps.length == 0) {
            return true; // No IP restriction
        }
        for (String allowedIp : allowedIps) {
            if (allowedIp.equals(ip)) {
                return true;
            }
        }
        return false;
    }
}
