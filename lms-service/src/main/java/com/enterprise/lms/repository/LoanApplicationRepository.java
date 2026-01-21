package com.enterprise.lms.repository;

import com.enterprise.lms.entity.LoanApplication;
import com.enterprise.lms.entity.LoanApplication.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    List<LoanApplication> findByCustomerId(UUID customerId);

    List<LoanApplication> findByStatus(ApplicationStatus status);

    List<LoanApplication> findByBranchId(UUID branchId);

    Page<LoanApplication> findByStatusIn(List<ApplicationStatus> statuses, Pageable pageable);

    Optional<LoanApplication> findByProcessInstanceId(String processInstanceId);

    @Query("SELECT a FROM LoanApplication a WHERE a.submittedBy = :userId ORDER BY a.createdAt DESC")
    List<LoanApplication> findBySubmittedByOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT MAX(CAST(SUBSTRING(a.applicationNumber, 9) AS int)) FROM LoanApplication a " +
            "WHERE a.applicationNumber LIKE :prefix%")
    Integer findMaxApplicationNumberSequence(@Param("prefix") String prefix);
}
