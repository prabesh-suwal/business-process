package com.cas.server.repository;

import com.cas.server.domain.session.CasSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CAS Sessions stored in Redis.
 */
@Repository
public interface CasSessionRepository extends CrudRepository<CasSession, String> {

    /**
     * Find all sessions for a user (for session management UI).
     */
    List<CasSession> findByUserId(UUID userId);
}
