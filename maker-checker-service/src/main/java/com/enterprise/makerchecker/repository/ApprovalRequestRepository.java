package com.enterprise.makerchecker.repository;

import com.enterprise.makerchecker.entity.ApprovalRequest;
import com.enterprise.makerchecker.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    Page<ApprovalRequest> findByMakerUserId(String makerUserId, Pageable pageable);

    Page<ApprovalRequest> findByStatusAndMakerUserId(ApprovalStatus status, String makerUserId, Pageable pageable);

    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = :status AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<ApprovalRequest> findExpiredApprovals(
            @Param("status") ApprovalStatus status,
            @Param("now") OffsetDateTime now);
}
