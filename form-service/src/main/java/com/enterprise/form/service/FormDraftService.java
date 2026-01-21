package com.enterprise.form.service;

import com.enterprise.form.dto.FormDraftDTO;
import com.enterprise.form.dto.SaveDraftRequest;
import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.entity.FormDraft;
import com.enterprise.form.repository.FormDefinitionRepository;
import com.enterprise.form.repository.FormDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing form drafts (partial saves).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FormDraftService {

    private final FormDraftRepository draftRepository;
    private final FormDefinitionRepository formRepository;

    @Value("${form.draft.expiry-days:30}")
    private int draftExpiryDays;

    /**
     * Save or update a draft.
     */
    public FormDraftDTO saveDraft(SaveDraftRequest request, UUID userId, String userName) {
        FormDefinition form = formRepository.findById(request.getFormDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Form not found: " + request.getFormDefinitionId()));

        // Find existing draft or create new
        FormDraft draft;
        if (request.getLinkedEntityId() != null) {
            draft = draftRepository.findByFormDefinitionIdAndUserIdAndLinkedEntityTypeAndLinkedEntityId(
                    form.getId(), userId, request.getLinkedEntityType(), request.getLinkedEntityId())
                    .orElse(null);
        } else {
            draft = draftRepository.findByFormDefinitionIdAndUserId(form.getId(), userId)
                    .orElse(null);
        }

        if (draft == null) {
            draft = FormDraft.builder()
                    .formDefinition(form)
                    .formVersion(form.getVersion())
                    .userId(userId)
                    .userName(userName)
                    .expiresAt(LocalDateTime.now().plusDays(draftExpiryDays))
                    .build();
        }

        // Update draft data
        draft.setFormData(request.getFormData());
        draft.setCompletedFields(request.getCompletedFields());
        draft.setCurrentStep(request.getCurrentStep());
        draft.setTotalSteps(request.getTotalSteps());
        draft.setLinkedEntityType(request.getLinkedEntityType());
        draft.setLinkedEntityId(request.getLinkedEntityId());
        draft.setContext(request.getContext());
        draft.setIsAutoSave(request.getIsAutoSave() != null ? request.getIsAutoSave() : false);

        draft = draftRepository.save(draft);

        log.debug("Saved draft {} for user {} on form {}",
                draft.getId(), userId, form.getName());

        return toDTO(draft);
    }

    /**
     * Get user's draft for a form.
     */
    @Transactional(readOnly = true)
    public Optional<FormDraftDTO> getDraft(UUID formDefinitionId, UUID userId) {
        return draftRepository.findByFormDefinitionIdAndUserId(formDefinitionId, userId)
                .filter(d -> !d.isExpired())
                .map(this::toDTO);
    }

    /**
     * Get user's draft for a form linked to entity.
     */
    @Transactional(readOnly = true)
    public Optional<FormDraftDTO> getDraft(UUID formDefinitionId, UUID userId,
            String linkedEntityType, UUID linkedEntityId) {
        return draftRepository.findByFormDefinitionIdAndUserIdAndLinkedEntityTypeAndLinkedEntityId(
                formDefinitionId, userId, linkedEntityType, linkedEntityId)
                .filter(d -> !d.isExpired())
                .map(this::toDTO);
    }

    /**
     * Get all drafts for a user.
     */
    @Transactional(readOnly = true)
    public List<FormDraftDTO> getUserDrafts(UUID userId) {
        return draftRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .filter(d -> !d.isExpired())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delete a draft.
     */
    public void deleteDraft(UUID draftId, UUID userId) {
        FormDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        if (!draft.getUserId().equals(userId)) {
            throw new IllegalStateException("Cannot delete another user's draft");
        }

        draftRepository.delete(draft);
        log.info("Deleted draft {} for user {}", draftId, userId);
    }

    /**
     * Delete draft when form is submitted successfully.
     */
    public void deleteDraftOnSubmission(UUID formDefinitionId, UUID userId,
            String linkedEntityType, UUID linkedEntityId) {
        Optional<FormDraft> draft;
        if (linkedEntityId != null) {
            draft = draftRepository.findByFormDefinitionIdAndUserIdAndLinkedEntityTypeAndLinkedEntityId(
                    formDefinitionId, userId, linkedEntityType, linkedEntityId);
        } else {
            draft = draftRepository.findByFormDefinitionIdAndUserId(formDefinitionId, userId);
        }

        draft.ifPresent(d -> {
            draftRepository.delete(d);
            log.info("Deleted draft after form submission: {}", d.getId());
        });
    }

    /**
     * Cleanup expired drafts (scheduled job).
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupExpiredDrafts() {
        int deleted = draftRepository.deleteExpiredDrafts(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired form drafts", deleted);
        }
    }

    private FormDraftDTO toDTO(FormDraft draft) {
        return FormDraftDTO.builder()
                .id(draft.getId())
                .formDefinitionId(draft.getFormDefinition().getId())
                .formName(draft.getFormDefinition().getName())
                .formVersion(draft.getFormVersion())
                .userId(draft.getUserId())
                .userName(draft.getUserName())
                .formData(draft.getFormData())
                .completedFields(draft.getCompletedFields())
                .currentStep(draft.getCurrentStep())
                .totalSteps(draft.getTotalSteps())
                .completionPercentage(draft.getCompletionPercentage())
                .linkedEntityType(draft.getLinkedEntityType())
                .linkedEntityId(draft.getLinkedEntityId())
                .context(draft.getContext())
                .isAutoSave(draft.getIsAutoSave())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .expiresAt(draft.getExpiresAt())
                .build();
    }
}
