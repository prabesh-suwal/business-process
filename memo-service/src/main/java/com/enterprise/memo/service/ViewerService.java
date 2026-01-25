package com.enterprise.memo.service;

import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.entity.WorkflowStepConfig;
import com.enterprise.memo.repository.MemoRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import com.enterprise.memo.repository.WorkflowStepConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for resolving viewer permissions.
 * Determines if a user can view memos or tasks based on viewer configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewerService {

    private final MemoRepository memoRepository;
    private final MemoTopicRepository memoTopicRepository;
    private final WorkflowStepConfigRepository stepConfigRepository;

    /**
     * Check if a user can view a specific memo.
     * 
     * @param memoId       The memo ID
     * @param userId       User's UUID
     * @param roles        User's roles (comma-separated or list)
     * @param departmentId User's department UUID
     * @return true if user can view the memo
     */
    public boolean canViewMemo(UUID memoId, String userId, List<String> roles, String departmentId) {
        log.debug("Checking view permission for memo: {}, user: {}, roles: {}, dept: {}",
                memoId, userId, roles, departmentId);

        Optional<Memo> memoOpt = memoRepository.findById(memoId);
        if (memoOpt.isEmpty()) {
            return false;
        }

        Memo memo = memoOpt.get();
        MemoTopic topic = memo.getTopic();

        // Check if user is the creator (creators can always view their memos)
        if (memo.getCreatedBy() != null && memo.getCreatedBy().toString().equals(userId)) {
            return true;
        }

        // Check memo-wide viewer configuration
        if (hasViewerAccess(topic.getViewerConfig(), userId, roles, departmentId)) {
            return true;
        }

        // If not a memo-wide viewer, check if they have access to ANY step
        List<WorkflowStepConfig> stepConfigs = stepConfigRepository.findByMemoTopicId(topic.getId());
        for (WorkflowStepConfig config : stepConfigs) {
            if (hasViewerAccess(config.getViewerConfig(), userId, roles, departmentId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a user can view a specific task/step.
     * 
     * @param taskKey      Task definition key
     * @param memoId       Memo ID
     * @param userId       User's UUID
     * @param roles        User's roles
     * @param departmentId User's department UUID
     * @return true if user can view the task
     */
    public boolean canViewTask(String taskKey, UUID memoId, String userId, List<String> roles, String departmentId) {
        Optional<Memo> memoOpt = memoRepository.findById(memoId);
        if (memoOpt.isEmpty()) {
            return false;
        }

        Memo memo = memoOpt.get();
        MemoTopic topic = memo.getTopic();

        // Check memo-wide viewer configuration first
        if (hasViewerAccess(topic.getViewerConfig(), userId, roles, departmentId)) {
            return true;
        }

        // Check step-specific viewer configuration
        Optional<WorkflowStepConfig> stepConfigOpt = stepConfigRepository
                .findByMemoTopicIdAndTaskKey(topic.getId(), taskKey);

        if (stepConfigOpt.isPresent()) {
            return hasViewerAccess(stepConfigOpt.get().getViewerConfig(), userId, roles, departmentId);
        }

        return false;
    }

    /**
     * Get list of memos that user can view (but may not be able to act on).
     * 
     * @param userId       User's UUID
     * @param roles        User's roles
     * @param departmentId User's department UUID
     * @return List of viewable memo IDs
     */
    public List<UUID> getViewableMemos(String userId, List<String> roles, String departmentId) {
        List<UUID> viewableMemoIds = new ArrayList<>();

        // Get all topics with viewer configurations
        List<MemoTopic> topicsWithViewers = memoTopicRepository.findAll().stream()
                .filter(topic -> topic.getViewerConfig() != null && !topic.getViewerConfig().isEmpty())
                .filter(topic -> hasViewerAccess(topic.getViewerConfig(), userId, roles, departmentId))
                .collect(Collectors.toList());

        // Get memos for these topics
        for (MemoTopic topic : topicsWithViewers) {
            List<Memo> memos = memoRepository.findByTopicId(topic.getId());
            viewableMemoIds.addAll(memos.stream().map(Memo::getId).collect(Collectors.toList()));
        }

        // Also get step-specific viewable memos
        List<WorkflowStepConfig> stepConfigsWithViewers = stepConfigRepository.findAll().stream()
                .filter(config -> config.getViewerConfig() != null && !config.getViewerConfig().isEmpty())
                .filter(config -> hasViewerAccess(config.getViewerConfig(), userId, roles, departmentId))
                .collect(Collectors.toList());

        for (WorkflowStepConfig config : stepConfigsWithViewers) {
            List<Memo> memos = memoRepository.findByTopicId(config.getMemoTopic().getId());
            viewableMemoIds.addAll(memos.stream().map(Memo::getId).collect(Collectors.toList()));
        }

        return viewableMemoIds.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Check if user has viewer access based on viewer configuration.
     * 
     * @param viewerConfig JSONB viewer configuration map
     * @param userId       User's UUID
     * @param roles        User's roles
     * @param departmentId User's department UUID
     * @return true if user matches any viewer rule
     */
    @SuppressWarnings("unchecked")
    private boolean hasViewerAccess(Map<String, Object> viewerConfig, String userId, List<String> roles,
            String departmentId) {
        if (viewerConfig == null || viewerConfig.isEmpty()) {
            return false;
        }

        List<Map<String, Object>> viewers = (List<Map<String, Object>>) viewerConfig.get("viewers");
        if (viewers == null || viewers.isEmpty()) {
            return false;
        }

        for (Map<String, Object> viewer : viewers) {
            String type = (String) viewer.get("type");

            switch (type) {
                case "USER":
                    String viewerUserId = (String) viewer.get("userId");
                    if (userId.equals(viewerUserId)) {
                        return true;
                    }
                    break;

                case "ROLE":
                    String viewerRole = (String) viewer.get("role");
                    if (roles != null && roles.contains(viewerRole)) {
                        return true;
                    }
                    break;

                case "DEPARTMENT":
                    String viewerDeptId = (String) viewer.get("departmentId");
                    if (departmentId != null && departmentId.equals(viewerDeptId)) {
                        return true;
                    }
                    break;

                default:
                    log.warn("Unknown viewer type: {}", type);
            }
        }

        return false;
    }
}
