package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.CreateWorkflowConfigRequest;
import com.enterprise.workflow.dto.WorkflowConfigurationDTO;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.WorkflowConfiguration;
import com.enterprise.workflow.repository.ProcessTemplateRepository;
import com.enterprise.workflow.repository.WorkflowConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConfigurationService {

    private final WorkflowConfigurationRepository configRepository;
    private final ProcessTemplateRepository processTemplateRepository;

    @Transactional
    public WorkflowConfigurationDTO createConfiguration(CreateWorkflowConfigRequest request) {
        if (configRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Configuration with code '" + request.getCode() + "' already exists");
        }

        WorkflowConfiguration config = new WorkflowConfiguration();
        config.setProductCode(request.getProductCode());
        config.setCode(request.getCode());
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setStartFormId(request.getStartFormId());
        config.setTaskFormMappings(
                request.getTaskFormMappings() != null ? request.getTaskFormMappings() : new HashMap<>());
        config.setAssignmentRules(
                request.getAssignmentRules() != null ? request.getAssignmentRules() : new HashMap<>());
        config.setConfig(request.getConfig() != null ? request.getConfig() : new HashMap<>());
        config.setActive(true);

        if (request.getProcessTemplateId() != null) {
            ProcessTemplate template = processTemplateRepository.findById(request.getProcessTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Process template not found"));
            config.setProcessTemplate(template);
        }

        config = configRepository.save(config);
        log.info("Created workflow configuration: {} for product: {}", config.getCode(), config.getProductCode());

        return toDTO(config);
    }

    @Transactional
    public WorkflowConfigurationDTO updateConfiguration(UUID id, CreateWorkflowConfigRequest request) {
        WorkflowConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        // Check code uniqueness if changed
        if (!config.getCode().equals(request.getCode()) && configRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Configuration with code '" + request.getCode() + "' already exists");
        }

        config.setProductCode(request.getProductCode());
        config.setCode(request.getCode());
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setStartFormId(request.getStartFormId());

        if (request.getTaskFormMappings() != null) {
            config.setTaskFormMappings(request.getTaskFormMappings());
        }
        if (request.getAssignmentRules() != null) {
            config.setAssignmentRules(request.getAssignmentRules());
        }
        if (request.getConfig() != null) {
            config.setConfig(request.getConfig());
        }

        if (request.getProcessTemplateId() != null) {
            ProcessTemplate template = processTemplateRepository.findById(request.getProcessTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Process template not found"));
            config.setProcessTemplate(template);
        }

        config = configRepository.save(config);
        log.info("Updated workflow configuration: {}", config.getCode());

        return toDTO(config);
    }

    public WorkflowConfigurationDTO getByCode(String code) {
        return configRepository.findByCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + code));
    }

    public WorkflowConfigurationDTO getById(UUID id) {
        return configRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
    }

    public List<WorkflowConfigurationDTO> getByProductCode(String productCode) {
        return configRepository.findByProductCode(productCode).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<WorkflowConfigurationDTO> getActiveByProductCode(String productCode) {
        return configRepository.findByProductCodeAndActive(productCode, true).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<WorkflowConfigurationDTO> getAllActive() {
        return configRepository.findByActive(true).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<WorkflowConfigurationDTO> getAll() {
        return configRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void activate(UUID id) {
        WorkflowConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
        config.setActive(true);
        configRepository.save(config);
        log.info("Activated workflow configuration: {}", config.getCode());
    }

    @Transactional
    public void deactivate(UUID id) {
        WorkflowConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
        config.setActive(false);
        configRepository.save(config);
        log.info("Deactivated workflow configuration: {}", config.getCode());
    }

    @Transactional
    public void delete(UUID id) {
        WorkflowConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
        configRepository.delete(config);
        log.info("Deleted workflow configuration: {}", config.getCode());
    }

    /**
     * Get the form ID for a specific task in a workflow configuration
     */
    public UUID getFormForTask(String configCode, String taskKey) {
        WorkflowConfiguration config = configRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configCode));

        if (config.getTaskFormMappings() == null) {
            return null;
        }

        return config.getTaskFormMappings().get(taskKey);
    }

    /**
     * Get the assignment rule for a specific task
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAssignmentRuleForTask(String configCode, String taskKey) {
        WorkflowConfiguration config = configRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configCode));

        if (config.getAssignmentRules() == null) {
            return null;
        }

        // Check for task-specific override
        Map<String, Object> taskOverrides = (Map<String, Object>) config.getAssignmentRules().get("taskOverrides");
        if (taskOverrides != null && taskOverrides.containsKey(taskKey)) {
            return (Map<String, Object>) taskOverrides.get(taskKey);
        }

        // Fall back to default assignment
        return (Map<String, Object>) config.getAssignmentRules().get("defaultAssignment");
    }

    /**
     * Add or update a task-form mapping
     */
    @Transactional
    public WorkflowConfigurationDTO setTaskFormMapping(UUID configId, String taskKey, UUID formId) {
        WorkflowConfiguration config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        if (config.getTaskFormMappings() == null) {
            config.setTaskFormMappings(new HashMap<>());
        }

        config.getTaskFormMappings().put(taskKey, formId);
        config = configRepository.save(config);
        log.info("Set form {} for task {} in config {}", formId, taskKey, config.getCode());

        return toDTO(config);
    }

    /**
     * Remove a task-form mapping
     */
    @Transactional
    public WorkflowConfigurationDTO removeTaskFormMapping(UUID configId, String taskKey) {
        WorkflowConfiguration config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        if (config.getTaskFormMappings() != null) {
            config.getTaskFormMappings().remove(taskKey);
            config = configRepository.save(config);
            log.info("Removed form mapping for task {} in config {}", taskKey, config.getCode());
        }

        return toDTO(config);
    }

    private WorkflowConfigurationDTO toDTO(WorkflowConfiguration config) {
        WorkflowConfigurationDTO dto = new WorkflowConfigurationDTO();
        dto.setId(config.getId());
        dto.setProductCode(config.getProductCode());
        dto.setCode(config.getCode());
        dto.setName(config.getName());
        dto.setDescription(config.getDescription());
        dto.setStartFormId(config.getStartFormId());
        dto.setTaskFormMappings(config.getTaskFormMappings());
        dto.setAssignmentRules(config.getAssignmentRules());
        dto.setConfig(config.getConfig());
        dto.setActive(config.isActive());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());

        if (config.getProcessTemplate() != null) {
            dto.setProcessTemplateId(config.getProcessTemplate().getId());
            dto.setProcessTemplateName(config.getProcessTemplate().getName());
        }

        return dto;
    }
}
