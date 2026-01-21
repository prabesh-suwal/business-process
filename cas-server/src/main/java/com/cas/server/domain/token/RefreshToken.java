package com.cas.server.domain.token;

import com.cas.server.domain.client.ApiClient;
import com.cas.server.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", unique = true, nullable = false, length = 255)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ApiClient client;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke the token.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    /**
     * Check if this is a user token.
     */
    public boolean isUserToken() {
        return user != null;
    }

    /**
     * Check if this is a client token.
     */
    public boolean isClientToken() {
        return client != null;
    }
}
