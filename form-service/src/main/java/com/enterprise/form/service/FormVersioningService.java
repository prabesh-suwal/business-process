package com.enterprise.form.service;

import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.entity.FormDefinition.FormStatus;
import com.enterprise.form.repository.FormDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing form versions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FormVersioningService {

    private final FormDefinitionRepository formRepository;

    /**
     * Create a new version of an existing form.
     * Copies the form and increments version number.
     */
    public FormDefinition createNewVersion(UUID formId, UUID createdBy, String createdByName) {
        FormDefinition existing = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        Integer nextVersion = formRepository
                .findMaxVersionByProductIdAndName(existing.getProductId(), existing.getName())
                .orElse(0) + 1;

        FormDefinition newVersion = FormDefinition.builder()
                .productId(existing.getProductId())
                .name(existing.getName())
                .description(existing.getDescription())
                .formType(existing.getFormType())
                .schema(existing.getSchema())
                .uiSchema(existing.getUiSchema())
                .layoutConfig(existing.getLayoutConfig())
                .validationRules(existing.getValidationRules())
                .version(nextVersion)
                .status(FormStatus.DRAFT)
                .previousVersionId(existing.getId())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        // Copy fields
        existing.getFields().forEach(field -> {
            // Field copying would be handled separately if needed
        });

        newVersion = formRepository.save(newVersion);
        log.info("Created new version {} of form {} ({})",
                nextVersion, existing.getName(), newVersion.getId());

        return newVersion;
    }

    /**
     * Publish a form (make it ACTIVE and immutable).
     */
    public FormDefinition publishForm(UUID formId, UUID publishedBy) {
        FormDefinition form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT forms can be published");
        }

        form.setStatus(FormStatus.ACTIVE);
        form.setPublishedAt(LocalDateTime.now());
        form.setPublishedBy(publishedBy);
        form = formRepository.save(form);

        log.info("Published form {} version {}", form.getName(), form.getVersion());
        return form;
    }

    /**
     * Deprecate a form.
     */
    public FormDefinition deprecateForm(UUID formId) {
        FormDefinition form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        form.setStatus(FormStatus.DEPRECATED);
        form = formRepository.save(form);

        log.info("Deprecated form {} version {}", form.getName(), form.getVersion());
        return form;
    }

    /**
     * Get the latest active version of a form.
     */
    @Transactional(readOnly = true)
    public Optional<FormDefinition> getLatestActiveForm(UUID productId, String name) {
        List<FormDefinition> forms = formRepository.findByProductIdAndNameAndStatusOrderByVersionDesc(
                productId, name, FormStatus.ACTIVE);
        return forms.isEmpty() ? Optional.empty() : Optional.of(forms.get(0));
    }

    /**
     * Get all versions of a form.
     */
    @Transactional(readOnly = true)
    public List<FormDefinition> getFormVersions(UUID productId, String name) {
        return formRepository.findByProductIdAndNameOrderByVersionDesc(productId, name);
    }

    /**
     * Get form by specific version.
     */
    @Transactional(readOnly = true)
    public Optional<FormDefinition> getFormByVersion(UUID productId, String name, Integer version) {
        return formRepository.findByProductIdAndNameAndVersion(productId, name, version);
    }
}
