package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.*;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.ProcessInstanceMetadata;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.ProcessTemplateForm;
import com.enterprise.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing tasks (inbox, complete, claim, delegate).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowTaskService {

    private final TaskService taskService;
    private final ProcessInstanceMetadataRepository processInstanceMetadataRepository;
    private final ProcessTemplateFormRepository processTemplateFormRepository;
    private final ActionTimelineRepository actionTimelineRepository;
    private final VariableAuditRepository variableAuditRepository;

    /**
     * Get tasks assigned to a specific user.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getAssignedTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks where user is a candidate (can claim).
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getCandidateTasks(String userId, List<String> groups) {
        TaskQuery query = taskService.createTaskQuery()
                .taskUnassigned();

        if (userId != null) {
            query.taskCandidateUser(userId);
        }
        if (groups != null && !groups.isEmpty()) {
            query.taskCandidateGroupIn(groups);
        }

        List<Task> tasks = query
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks for a specific process instance.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProcessInstance(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();

        return tasks.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get tasks for a product (all tasks from processes of that product).
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProduct(UUID productId, String assignee) {
        // Get all process instances for this product
        List<ProcessInstanceMetadata> instances = processInstanceMetadataRepository
                .findByProductIdOrderByStartedAtDesc(productId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (instances.isEmpty()) {
            return List.of();
        }

        List<String> processInstanceIds = instances.stream()
                .map(ProcessInstanceMetadata::getFlowableProcessInstanceId)
                .toList();

        TaskQuery query = taskService.createTaskQuery()
                .processInstanceIdIn(processInstanceIds);

        if (assignee != null) {
            query.taskAssignee(assignee);
        }

        return query.orderByTaskCreateTime().desc().list().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get a specific task by ID.
     */
    @Transactional(readOnly = true)
    public TaskDTO getTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        return toDTO(task);
    }

    /**
     * Claim a task from the pool.
     */
    public TaskDTO claimTask(String taskId, String userId, String userName) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (task.getAssignee() != null) {
            throw new IllegalStateException("Task is already assigned to: " + task.getAssignee());
        }

        taskService.claim(taskId, userId);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_CLAIMED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(UUID.fromString(userId))
                .actorName(userName)
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Task {} claimed by user {}", taskId, userName);

        return getTask(taskId);
    }

    /**
     * Unclaim (release) a task back to the pool.
     */
    public void unclaimTask(String taskId, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Release the task (set assignee to null)
        taskService.unclaim(taskId);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_CLAIMED) // Reusing for unclaim event
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(UUID.fromString(userId))
                .metadata(Map.of("action", "unclaimed"))
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Task {} unclaimed by user {}", taskId, userId);
    }

    /**
     * Complete a task with variables.
     */
    public void completeTask(String taskId, CompleteTaskRequest request, UUID userId, String userName,
            List<String> userRoles) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Add comment if provided
        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment());
        }

        // Complete the task with variables
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            taskService.complete(taskId, request.getVariables());
        } else {
            taskService.complete(taskId);
        }

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_COMPLETED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(userId)
                .actorName(userName)
                .actorRoles(userRoles)
                .metadata(Map.of(
                        "approved", request.isApproved(),
                        "hasComment", request.getComment() != null))
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Task {} completed by user {}. Approved: {}", taskId, userName, request.isApproved());
    }

    /**
     * Delegate a task to another user.
     */
    public void delegateTask(String taskId, String delegateTo, UUID delegatedBy, String delegatedByName) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        taskService.delegateTask(taskId, delegateTo);

        // Record in timeline
        ActionTimeline timelineEvent = ActionTimeline.builder()
                .processInstanceId(task.getProcessInstanceId())
                .actionType(ActionTimeline.ActionType.TASK_DELEGATED)
                .taskId(taskId)
                .taskName(task.getName())
                .actorId(delegatedBy)
                .actorName(delegatedByName)
                .metadata(Map.of("delegateTo", delegateTo))
                .build();
        actionTimelineRepository.save(timelineEvent);

        log.info("Task {} delegated to {} by user {}", taskId, delegateTo, delegatedByName);
    }

    /**
     * Set task priority.
     */
    public void setTaskPriority(String taskId, int priority) {
        taskService.setPriority(taskId, priority);
        log.debug("Set priority {} on task {}", priority, taskId);
    }

    private TaskDTO toDTO(Task task) {
        // Try to get process metadata for additional context
        ProcessInstanceMetadata metadata = null;
        ProcessTemplate template = null;
        UUID formDefinitionId = null;

        try {
            metadata = processInstanceMetadataRepository
                    .findByFlowableProcessInstanceId(task.getProcessInstanceId())
                    .orElse(null);

            if (metadata != null && metadata.getProcessTemplate() != null) {
                template = metadata.getProcessTemplate();

                // Get form mapping for this task
                ProcessTemplateForm formMapping = processTemplateFormRepository
                        .findByProcessTemplateIdAndTaskKey(template.getId(), task.getTaskDefinitionKey())
                        .orElse(null);

                if (formMapping != null) {
                    formDefinitionId = formMapping.getFormDefinitionId();
                }
            }
        } catch (Exception e) {
            log.warn("Error loading process metadata for task {}: {}", task.getId(), e.getMessage());
        }

        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processTemplateId(template != null ? template.getId() : null)
                .processTemplateName(template != null ? template.getName() : null)
                .productId(metadata != null ? metadata.getProductId() : null)
                .businessKey(metadata != null ? metadata.getBusinessKey() : null)
                .processTitle(metadata != null ? metadata.getTitle() : null)
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .priority(task.getPriority())
                .createTime(task.getCreateTime() != null
                        ? task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .claimTime(task.getClaimTime() != null
                        ? task.getClaimTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .formDefinitionId(formDefinitionId)
                .build();
    }
}
