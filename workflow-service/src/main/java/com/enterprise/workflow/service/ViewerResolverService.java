package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.TaskConfigurationDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generic Viewer Resolver Service.
 * 
 * Centralized viewer resolution logic that works across all products (MMS, LMS,
 * etc.).
 * Determines who can view tasks/entities based on viewer configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewerResolverService {

    private final TaskConfigurationService taskConfigService;

    /**
     * Check if a user can view a task based on viewer configuration.
     * 
     * @param processTemplateId The workflow template ID
     * @param taskKey           The BPMN task definition key
     * @param userId            The user ID to check
     * @param userRoles         The user's roles
     * @param userDepartment    The user's department
     * @return true if user can view the task
     */
    public boolean canViewTask(
            UUID processTemplateId,
            String taskKey,
            String userId,
            List<String> userRoles,
            String userDepartment) {

        log.debug("Checking view access for user {} on template {} task {}",
                userId, processTemplateId, taskKey);

        // Get task configuration
        TaskConfigurationDTO taskConfig;
        try {
            taskConfig = taskConfigService.getTaskConfig(processTemplateId, taskKey);
        } catch (IllegalArgumentException e) {
            log.debug("No task config found, denying view access");
            return false;
        }

        Map<String, Object> viewerConfig = taskConfig.getViewerConfig();
        if (viewerConfig == null || viewerConfig.isEmpty()) {
            log.debug("No viewer config, allowing default access");
            return true; // No config = allow all
        }

        return checkViewerAccess(viewerConfig, userId, userRoles, userDepartment);
    }

    /**
     * Resolve all viewers for a task based on configuration.
     * 
     * @param processTemplateId The workflow template ID
     * @param taskKey           The BPMN task definition key
     * @return ViewerResult with roles, departments, and users who can view
     */
    public ViewerResult resolveViewers(UUID processTemplateId, String taskKey) {
        log.info("Resolving viewers for template {} task {}", processTemplateId, taskKey);

        // Get task configuration
        TaskConfigurationDTO taskConfig;
        try {
            taskConfig = taskConfigService.getTaskConfig(processTemplateId, taskKey);
        } catch (IllegalArgumentException e) {
            log.warn("No task config found for {} {}", processTemplateId, taskKey);
            return ViewerResult.builder().build();
        }

        Map<String, Object> viewerConfig = taskConfig.getViewerConfig();
        if (viewerConfig == null || viewerConfig.isEmpty()) {
            return ViewerResult.builder().build();
        }

        return parseViewerConfig(viewerConfig);
    }

    /**
     * Check if user has viewer access based on config.
     */
    private boolean checkViewerAccess(
            Map<String, Object> viewerConfig,
            String userId,
            List<String> userRoles,
            String userDepartment) {

        List<Map<String, Object>> viewers = getViewersList(viewerConfig);
        if (viewers.isEmpty()) {
            return true; // No specific viewers = allow all
        }

        for (Map<String, Object> viewer : viewers) {
            String type = (String) viewer.get("type");
            if (type == null)
                continue;

            switch (type.toUpperCase()) {
                case "ROLE" -> {
                    String role = (String) viewer.get("role");
                    if (role != null && userRoles != null && userRoles.contains(role)) {
                        log.debug("User has viewer role: {}", role);
                        return true;
                    }
                }
                case "DEPARTMENT" -> {
                    String dept = (String) viewer.get("departmentId");
                    if (dept != null && dept.equals(userDepartment)) {
                        log.debug("User is in viewer department: {}", dept);
                        return true;
                    }
                }
                case "USER" -> {
                    String viewerUserId = (String) viewer.get("userId");
                    if (viewerUserId != null && viewerUserId.equals(userId)) {
                        log.debug("User is specific viewer: {}", userId);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Parse viewer config into structured result.
     */
    private ViewerResult parseViewerConfig(Map<String, Object> viewerConfig) {
        List<String> roles = new ArrayList<>();
        List<String> departments = new ArrayList<>();
        List<String> users = new ArrayList<>();

        List<Map<String, Object>> viewers = getViewersList(viewerConfig);

        for (Map<String, Object> viewer : viewers) {
            String type = (String) viewer.get("type");
            if (type == null)
                continue;

            switch (type.toUpperCase()) {
                case "ROLE" -> {
                    String role = (String) viewer.get("role");
                    if (role != null)
                        roles.add(role);
                }
                case "DEPARTMENT" -> {
                    String dept = (String) viewer.get("departmentId");
                    if (dept != null)
                        departments.add(dept);
                }
                case "USER" -> {
                    String userId = (String) viewer.get("userId");
                    if (userId != null)
                        users.add(userId);
                }
            }
        }

        return ViewerResult.builder()
                .roles(roles)
                .departments(departments)
                .users(users)
                .build();
    }

    /**
     * Extract viewers list from config.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getViewersList(Map<String, Object> viewerConfig) {
        Object viewers = viewerConfig.get("viewers");
        if (viewers instanceof List) {
            return (List<Map<String, Object>>) viewers;
        }
        return Collections.emptyList();
    }

    /**
     * Result of viewer resolution.
     */
    @Data
    @Builder
    public static class ViewerResult {
        @Builder.Default
        private List<String> roles = new ArrayList<>();
        @Builder.Default
        private List<String> departments = new ArrayList<>();
        @Builder.Default
        private List<String> users = new ArrayList<>();
    }
}
