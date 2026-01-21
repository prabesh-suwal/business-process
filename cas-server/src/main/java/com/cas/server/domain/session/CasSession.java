package com.cas.server.domain.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * CAS Session stored in Redis for SSO.
 * When a user logs in at CAS, a session is created.
 * Subsequent OAuth authorize requests check for this session
 * and auto-approve if valid (no re-login required).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("cas_session")
public class CasSession implements Serializable {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private String username;
    private String email;
    private String displayName;

    private Instant loginTime;
    private Instant lastActivity;

    private String ipAddress;
    private String userAgent;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttlSeconds;

    /**
     * Create a new session for a user.
     */
    public static CasSession create(UUID userId, String username, String email,
            String displayName, String ipAddress,
            String userAgent, long ttlSeconds) {
        Instant now = Instant.now();
        return CasSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .email(email)
                .displayName(displayName)
                .loginTime(now)
                .lastActivity(now)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    /**
     * Update last activity time.
     */
    public void touch() {
        this.lastActivity = Instant.now();
    }
}
