package com.enterprise.workflow.service;

import com.enterprise.workflow.entity.WorkflowVariable;
import com.enterprise.workflow.repository.WorkflowVariableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowVariableService {

    private final WorkflowVariableRepository repository;

    public List<WorkflowVariable> getAllVariables() {
        return repository.findAll();
    }

    public WorkflowVariable getByKey(String key) {
        return repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Workflow variable not found: " + key));
    }

    @Transactional
    @CacheEvict(value = "workflowVariables", allEntries = true)
    public WorkflowVariable createOrUpdate(WorkflowVariable variable) {
        return repository.findByKey(variable.getKey())
                .map(existing -> {
                    existing.setValue(variable.getValue());
                    existing.setLabel(variable.getLabel());
                    existing.setType(variable.getType());
                    existing.setDescription(variable.getDescription());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(variable));
    }

    @Transactional
    @CacheEvict(value = "workflowVariables", allEntries = true)
    public void deleteVariable(String key) {
        repository.findByKey(key).ifPresent(repository::delete);
    }

    /**
     * Get all variables as a Map for process injection.
     */
    @Cacheable("workflowVariables")
    public Map<String, Object> getGlobalVariables() {
        Map<String, Object> variables = new HashMap<>();
        List<WorkflowVariable> all = repository.findAll();

        for (WorkflowVariable var : all) {
            // Convert types if necessary (e.g. numbers)
            Object value = var.getValue();
            try {
                if (var.getType().equals("NUMBER")) {
                    value = Long.parseLong(var.getValue());
                } else if (var.getType().equals("BOOLEAN")) {
                    value = Boolean.parseBoolean(var.getValue());
                }
            } catch (Exception e) {
                log.warn("Failed to parse variable {} as type {}, utilizing string value", var.getKey(), var.getType());
            }
            variables.put(var.getKey(), value);
        }
        return variables;
    }
}
