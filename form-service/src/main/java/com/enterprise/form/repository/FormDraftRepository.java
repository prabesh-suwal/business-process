package com.enterprise.form.repository;

import com.enterprise.form.entity.FormDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormDraftRepository extends JpaRepository<FormDraft, UUID> {

    /**
     * Find user's draft for a specific form.
     */
    Optional<FormDraft> findByFormDefinitionIdAndUserId(UUID formDefinitionId, UUID userId);

    /**
     * Find user's draft for a form linked to a specific entity.
     */
    Optional<FormDraft> findByFormDefinitionIdAndUserIdAndLinkedEntityTypeAndLinkedEntityId(
            UUID formDefinitionId, UUID userId, String linkedEntityType, UUID linkedEntityId);

    /**
     * Find all drafts for a user.
     */
    List<FormDraft> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Find drafts linked to an entity.
     */
    List<FormDraft> findByLinkedEntityTypeAndLinkedEntityId(String entityType, UUID entityId);

    /**
     * Delete expired drafts.
     */
    @Modifying
    @Query("DELETE FROM FormDraft d WHERE d.expiresAt < :now")
    int deleteExpiredDrafts(@Param("now") LocalDateTime now);

    /**
     * Count user's drafts.
     */
    long countByUserId(UUID userId);
}
