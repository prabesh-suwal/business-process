package com.enterprise.workflow.service;

import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.ProcessTemplate.ProcessTemplateStatus;
import com.enterprise.workflow.repository.ProcessTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing process template versions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProcessVersioningService {

    private final ProcessTemplateRepository templateRepository;

    /**
     * Create a new version of an existing template.
     * Copies the template and increments version number.
     */
    public ProcessTemplate createNewVersion(UUID templateId, UUID createdBy, String createdByName) {
        ProcessTemplate existing = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        Integer nextVersion = templateRepository
                .findMaxVersionByProductIdAndName(existing.getProductId(), existing.getName())
                .orElse(0) + 1;

        ProcessTemplate newVersion = ProcessTemplate.builder()
                .productId(existing.getProductId())
                .name(existing.getName())
                .description(existing.getDescription())
                .bpmnXml(existing.getBpmnXml())
                .version(nextVersion)
                .status(ProcessTemplateStatus.DRAFT)
                .previousVersionId(existing.getId())
                .startFormId(existing.getStartFormId())
                .startFormVersion(existing.getStartFormVersion())
                .defaultSlaHours(existing.getDefaultSlaHours())
                .config(existing.getConfig())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        newVersion = templateRepository.save(newVersion);
        log.info("Created new version {} of template {} ({})",
                nextVersion, existing.getName(), newVersion.getId());

        return newVersion;
    }

    /**
     * Activate a template version with effective dates.
     */
    public ProcessTemplate activateVersion(UUID templateId, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        ProcessTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (template.getFlowableProcessDefKey() == null) {
            throw new IllegalStateException("Template must be deployed to Flowable before activation");
        }

        // Deprecate other active versions of the same template
        List<ProcessTemplate> activeVersions = templateRepository.findByProductIdAndStatusOrderByNameAsc(
                template.getProductId(), ProcessTemplateStatus.ACTIVE);

        for (ProcessTemplate active : activeVersions) {
            if (active.getName().equals(template.getName()) && !active.getId().equals(template.getId())) {
                // Set effectiveTo on the previous version
                if (active.getEffectiveTo() == null && effectiveFrom != null) {
                    active.setEffectiveTo(effectiveFrom);
                    templateRepository.save(active);
                    log.info("Set effectiveTo on previous version {}", active.getId());
                }
            }
        }

        template.setStatus(ProcessTemplateStatus.ACTIVE);
        template.setEffectiveFrom(effectiveFrom != null ? effectiveFrom : LocalDateTime.now());
        template.setEffectiveTo(effectiveTo);
        template = templateRepository.save(template);

        log.info("Activated template version {} effective from {}",
                templateId, template.getEffectiveFrom());

        return template;
    }

    /**
     * Deprecate a template version.
     */
    public ProcessTemplate deprecateVersion(UUID templateId) {
        ProcessTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        template.setStatus(ProcessTemplateStatus.DEPRECATED);
        template.setEffectiveTo(LocalDateTime.now());
        template = templateRepository.save(template);

        log.info("Deprecated template version {}", templateId);
        return template;
    }

    /**
     * Get the currently effective template for a product and name.
     */
    @Transactional(readOnly = true)
    public Optional<ProcessTemplate> getEffectiveTemplate(UUID productId, String name) {
        List<ProcessTemplate> effective = templateRepository.findEffectiveByProductIdAndName(
                productId, name, LocalDateTime.now());

        return effective.isEmpty() ? Optional.empty() : Optional.of(effective.get(0));
    }

    /**
     * Get all effective templates for a product.
     */
    @Transactional(readOnly = true)
    public List<ProcessTemplate> getAllEffectiveTemplates(UUID productId) {
        return templateRepository.findAllEffectiveByProductId(productId, LocalDateTime.now());
    }

    /**
     * Get version history of a template.
     */
    @Transactional(readOnly = true)
    public List<ProcessTemplate> getVersionHistory(UUID productId, String name) {
        return templateRepository.findByProductIdAndNameOrderByVersionDesc(productId, name);
    }
}
