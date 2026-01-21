package com.cas.server.domain.token;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    private UUID jti;

    @CreationTimestamp
    @Column(name = "revoked_at", updatable = false)
    private Instant revokedAt;

    @Column(length = 255)
    private String reason;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
