package com.enterprise.form.service;

import com.enterprise.form.dto.*;
import com.enterprise.form.entity.FieldDefinition;
import com.enterprise.form.entity.FormDefinition;
import com.enterprise.form.repository.FieldDefinitionRepository;
import com.enterprise.form.repository.FormDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for creating and managing form definitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FormBuilderService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;

    /**
     * Create a new form definition.
     */
    public FormDefinitionDTO createForm(CreateFormRequest request, UUID createdBy) {
        // Determine version
        Integer maxVersion = formDefinitionRepository
                .findMaxVersionByProductIdAndName(request.getProductId(), request.getName())
                .orElse(0);

        FormDefinition form = FormDefinition.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .description(request.getDescription())
                .schema(request.getSchema())
                .uiSchema(request.getUiSchema())
                .version(maxVersion + 1)
                .status(FormDefinition.FormStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        form = formDefinitionRepository.save(form);

        // Create field definitions if provided
        if (request.getFields() != null && !request.getFields().isEmpty()) {
            final FormDefinition savedForm = form;
            List<FieldDefinition> fields = request.getFields().stream()
                    .map(dto -> createFieldFromDTO(dto, savedForm))
                    .collect(Collectors.toList());
            fieldDefinitionRepository.saveAll(fields);
            form.setFields(fields);
        }

        log.info("Created form definition: {} (version {})", form.getName(), form.getVersion());
        return toDTO(form);
    }

    /**
     * Update a form definition (DRAFT only).
     */
    public FormDefinitionDTO updateForm(UUID formId, CreateFormRequest request) {
        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        if (form.getStatus() != FormDefinition.FormStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT forms can be updated. Status: " + form.getStatus());
        }

        if (request.getName() != null) {
            form.setName(request.getName());
        }
        if (request.getDescription() != null) {
            form.setDescription(request.getDescription());
        }
        if (request.getSchema() != null) {
            form.setSchema(request.getSchema());
        }
        if (request.getUiSchema() != null) {
            form.setUiSchema(request.getUiSchema());
        }

        // Update fields if provided
        if (request.getFields() != null) {
            // Clear existing fields (properly handles orphanRemoval=true)
            form.getFields().clear();

            // Add new fields to the existing collection
            final FormDefinition formForLambda = form;
            List<FieldDefinition> newFields = request.getFields().stream()
                    .map(dto -> createFieldFromDTO(dto, formForLambda))
                    .collect(Collectors.toList());
            form.getFields().addAll(newFields);
        }

        form = formDefinitionRepository.save(form);
        log.info("Updated form definition: {}", formId);
        return toDTO(form);
    }

    /**
     * Get form definition by ID.
     */
    @Transactional(readOnly = true)
    public FormDefinitionDTO getForm(UUID formId) {
        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));
        return toDTO(form);
    }

    /**
     * Get forms by product.
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getFormsByProduct(UUID productId) {
        return formDefinitionRepository.findByProductIdOrderByNameAsc(productId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active forms by product.
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getActiveFormsByProduct(UUID productId) {
        return formDefinitionRepository
                .findByProductIdAndStatusOrderByNameAsc(productId, FormDefinition.FormStatus.ACTIVE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Activate a form (make it ACTIVE).
     */
    public FormDefinitionDTO activateForm(UUID formId) {
        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        form.setStatus(FormDefinition.FormStatus.ACTIVE);
        form = formDefinitionRepository.save(form);
        log.info("Activated form: {}", formId);
        return toDTO(form);
    }

    /**
     * Deprecate a form.
     */
    public FormDefinitionDTO deprecateForm(UUID formId) {
        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        form.setStatus(FormDefinition.FormStatus.DEPRECATED);
        form = formDefinitionRepository.save(form);
        log.info("Deprecated form: {}", formId);
        return toDTO(form);
    }

    /**
     * Delete a form (DRAFT only).
     */
    public void deleteForm(UUID formId) {
        FormDefinition form = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        if (form.getStatus() != FormDefinition.FormStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT forms can be deleted. Status: " + form.getStatus());
        }

        formDefinitionRepository.delete(form);
        log.info("Deleted form: {}", formId);
    }

    private FieldDefinition createFieldFromDTO(FieldDefinitionDTO dto, FormDefinition form) {
        return FieldDefinition.builder()
                .formDefinition(form)
                .fieldKey(dto.getFieldKey())
                .fieldType(dto.getFieldType())
                .label(dto.getLabel())
                .placeholder(dto.getPlaceholder())
                .helpText(dto.getHelpText())
                .required(dto.getRequired() != null && dto.getRequired())
                .validationRules(dto.getValidationRules())
                .visibilityRules(dto.getVisibilityRules())
                .options(dto.getOptions())
                .defaultValue(dto.getDefaultValue())
                .displayOrder(dto.getDisplayOrder())
                .groupName(dto.getGroupName())
                // Layout properties
                .elementType(dto.getElementType() != null ? dto.getElementType() : "field")
                .width(dto.getWidth() != null ? dto.getWidth() : "full")
                .customWidth(dto.getCustomWidth())
                .customHeight(dto.getCustomHeight())
                .labelPosition(dto.getLabelPosition() != null ? dto.getLabelPosition() : "top")
                .sectionId(dto.getSectionId())
                .build();
    }

    private FormDefinitionDTO toDTO(FormDefinition form) {
        List<FieldDefinitionDTO> fieldDTOs = form.getFields().stream()
                .map(this::toFieldDTO)
                .collect(Collectors.toList());

        return FormDefinitionDTO.builder()
                .id(form.getId())
                .productId(form.getProductId())
                .name(form.getName())
                .description(form.getDescription())
                .version(form.getVersion())
                .schema(form.getSchema())
                .uiSchema(form.getUiSchema())
                .status(form.getStatus())
                .createdBy(form.getCreatedBy())
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .fields(fieldDTOs)
                .build();
    }

    private FieldDefinitionDTO toFieldDTO(FieldDefinition field) {
        return FieldDefinitionDTO.builder()
                .id(field.getId())
                .fieldKey(field.getFieldKey())
                .fieldType(field.getFieldType())
                .label(field.getLabel())
                .placeholder(field.getPlaceholder())
                .helpText(field.getHelpText())
                .required(field.getRequired())
                .validationRules(field.getValidationRules())
                .visibilityRules(field.getVisibilityRules())
                .options(field.getOptions())
                .defaultValue(field.getDefaultValue())
                .displayOrder(field.getDisplayOrder())
                .groupName(field.getGroupName())
                // Layout properties
                .elementType(field.getElementType())
                .width(field.getWidth())
                .customWidth(field.getCustomWidth())
                .customHeight(field.getCustomHeight())
                .labelPosition(field.getLabelPosition())
                .sectionId(field.getSectionId())
                .build();
    }
}
