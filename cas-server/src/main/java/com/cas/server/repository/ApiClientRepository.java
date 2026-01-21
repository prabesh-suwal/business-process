package com.cas.server.repository;

import com.cas.server.domain.client.ApiClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

    Optional<ApiClient> findByClientId(String clientId);

    boolean existsByClientId(String clientId);

    @Query("SELECT c FROM ApiClient c LEFT JOIN FETCH c.scopes WHERE c.clientId = :clientId")
    Optional<ApiClient> findByClientIdWithScopes(String clientId);

    Page<ApiClient> findByStatus(ApiClient.ClientStatus status, Pageable pageable);

    @Query("SELECT c FROM ApiClient c WHERE c.expiresAt < :now AND c.status = 'ACTIVE'")
    List<ApiClient> findExpiredClients(Instant now);

    @Query("SELECT c FROM ApiClient c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.clientId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ApiClient> searchByNameOrClientId(String search, Pageable pageable);
}
