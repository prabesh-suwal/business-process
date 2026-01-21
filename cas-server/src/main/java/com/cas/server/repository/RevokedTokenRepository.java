package com.cas.server.repository;

import com.cas.server.domain.token.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {

    boolean existsByJti(UUID jti);

    @Modifying
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
