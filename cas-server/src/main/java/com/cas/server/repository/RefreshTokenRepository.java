package com.cas.server.repository;

import com.cas.server.domain.token.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidToken(String tokenHash, Instant now);

    List<RefreshToken> findByUserIdAndRevoked(UUID userId, boolean revoked);

    List<RefreshToken> findByClientIdAndRevoked(UUID clientId, boolean revoked);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user.id = :userId")
    void revokeAllByUserId(UUID userId, Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.client.id = :clientId")
    void revokeAllByClientId(UUID clientId, Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
