package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.CommitteeVoteDTO;
import com.enterprise.workflow.dto.CommitteeVoteDTO.VoteRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for handling committee voting on workflow tasks.
 * Tracks individual votes and determines when voting threshold is met.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommitteeVotingService {

    private final TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Variable names for storing vote data
    private static final String VAR_VOTES = "committeeVotes";
    private static final String VAR_APPROVALS = "approvalCount";
    private static final String VAR_REJECTIONS = "rejectionCount";
    private static final String VAR_VOTE_STATUS = "voteStatus";

    /**
     * Cast a vote for a committee task.
     * Returns the updated vote status.
     */
    public CommitteeVoteDTO castVote(String taskId, String voterId, String voterName,
            String decision, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        // Get current votes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> votes = (List<Map<String, Object>>) taskService
                .getVariable(taskId, VAR_VOTES);
        if (votes == null) {
            votes = new ArrayList<>();
        }

        // Check if user already voted
        boolean alreadyVoted = votes.stream()
                .anyMatch(v -> voterId.equals(v.get("voterId")));
        if (alreadyVoted) {
            throw new RuntimeException("User has already voted on this task");
        }

        // Add vote
        Map<String, Object> vote = new HashMap<>();
        vote.put("voterId", voterId);
        vote.put("voterName", voterName);
        vote.put("decision", decision);
        vote.put("comment", comment);
        vote.put("votedAt", Instant.now().toString());
        votes.add(vote);

        // Update vote counts
        int approvals = (int) votes.stream()
                .filter(v -> "APPROVE".equals(v.get("decision")))
                .count();
        int rejections = (int) votes.stream()
                .filter(v -> "REJECT".equals(v.get("decision")))
                .count();

        // Save to task variables
        taskService.setVariable(taskId, VAR_VOTES, votes);
        taskService.setVariable(taskId, VAR_APPROVALS, approvals);
        taskService.setVariable(taskId, VAR_REJECTIONS, rejections);

        log.info("Vote cast on task {}: {} by {} (approvals={}, rejections={})",
                taskId, decision, voterId, approvals, rejections);

        // Check if voting is complete
        CommitteeVoteDTO voteStatus = getVoteStatus(taskId);
        checkAndCompleteVoting(taskId, voteStatus);

        return voteStatus;
    }

    /**
     * Get current voting status for a committee task.
     */
    public CommitteeVoteDTO getVoteStatus(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        // Get committee config from task description
        String docJson = task.getDescription();
        CommitteeConfig config = parseCommitteeConfig(docJson);

        int requiredApprovals = getRequiredApprovals(config);
        int totalMembers = config.totalMembers != null ? config.totalMembers : 5;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> votes = (List<Map<String, Object>>) taskService
                .getVariable(taskId, VAR_VOTES);
        if (votes == null) {
            votes = new ArrayList<>();
        }

        int approvals = (int) votes.stream()
                .filter(v -> "APPROVE".equals(v.get("decision")))
                .count();
        int rejections = (int) votes.stream()
                .filter(v -> "REJECT".equals(v.get("decision")))
                .count();

        String status = determineStatus(approvals, rejections, requiredApprovals, totalMembers, config);

        List<VoteRecord> voteRecords = votes.stream()
                .map(this::toVoteRecord)
                .toList();

        return CommitteeVoteDTO.builder()
                .taskId(taskId)
                .committeeCode(config.committeeCode)
                .requiredApprovals(requiredApprovals)
                .totalMembers(totalMembers)
                .approvalsReceived(approvals)
                .rejectionsReceived(rejections)
                .status(status)
                .votes(voteRecords)
                .build();
    }

    /**
     * Initialize voting variables for a new committee task.
     */
    public void initializeVoting(String taskId, String committeeCode, int totalMembers) {
        taskService.setVariable(taskId, VAR_VOTES, new ArrayList<>());
        taskService.setVariable(taskId, VAR_APPROVALS, 0);
        taskService.setVariable(taskId, VAR_REJECTIONS, 0);
        taskService.setVariable(taskId, VAR_VOTE_STATUS, "PENDING");

        log.info("Initialized voting for committee task: {} (committee={})", taskId, committeeCode);
    }

    /**
     * Get required approvals based on decision rule.
     */
    private int getRequiredApprovals(CommitteeConfig config) {
        if ("THRESHOLD".equals(config.decisionRule) && config.requiredApprovals != null) {
            return config.requiredApprovals;
        }

        int total = config.totalMembers != null ? config.totalMembers : 5;

        return switch (config.decisionRule != null ? config.decisionRule : "MAJORITY") {
            case "UNANIMOUS" -> total;
            case "MAJORITY" -> (total / 2) + 1;
            case "THRESHOLD" -> config.requiredApprovals != null ? config.requiredApprovals : (total / 2) + 1;
            default -> (total / 2) + 1;
        };
    }

    /**
     * Determine voting status.
     */
    private String determineStatus(int approvals, int rejections, int required,
            int total, CommitteeConfig config) {
        if (approvals >= required) {
            return "APPROVED";
        }

        // Check if rejection threshold met (for unanimous, any rejection fails)
        if ("UNANIMOUS".equals(config.decisionRule) && rejections > 0) {
            return "REJECTED";
        }

        // Check if it's mathematically impossible to reach approval threshold
        int remainingVotes = total - approvals - rejections;
        if (approvals + remainingVotes < required) {
            return "REJECTED";
        }

        return "PENDING";
    }

    /**
     * Check if voting is complete and complete the task if so.
     */
    private void checkAndCompleteVoting(String taskId, CommitteeVoteDTO voteStatus) {
        if (voteStatus.isVotingComplete()) {
            taskService.setVariable(taskId, VAR_VOTE_STATUS, voteStatus.getStatus());

            // Complete the task with the voting result
            Map<String, Object> variables = new HashMap<>();
            variables.put("committeeDecision", voteStatus.getStatus());
            variables.put("approvalCount", voteStatus.getApprovalsReceived());
            variables.put("rejectionCount", voteStatus.getRejectionsReceived());

            taskService.complete(taskId, variables);
            log.info("Committee voting complete for task {}: {}", taskId, voteStatus.getStatus());
        }
    }

    /**
     * Check if a task is a committee voting task.
     */
    public boolean isCommitteeTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
            return false;

        String candidateGroups = (String) taskService.getVariable(taskId, "candidateGroups");
        return candidateGroups != null && candidateGroups.startsWith("COMMITTEE_");
    }

    private VoteRecord toVoteRecord(Map<String, Object> vote) {
        return VoteRecord.builder()
                .voterId((String) vote.get("voterId"))
                .voterName((String) vote.get("voterName"))
                .decision((String) vote.get("decision"))
                .comment((String) vote.get("comment"))
                .votedAt(vote.get("votedAt") != null ? Instant.parse((String) vote.get("votedAt")) : null)
                .build();
    }

    private CommitteeConfig parseCommitteeConfig(String json) {
        if (json == null || json.isBlank()) {
            return CommitteeConfig.builder()
                    .decisionRule("MAJORITY")
                    .totalMembers(5)
                    .build();
        }
        try {
            return objectMapper.readValue(json, CommitteeConfig.class);
        } catch (Exception e) {
            log.warn("Failed to parse committee config: {}", e.getMessage());
            return CommitteeConfig.builder()
                    .decisionRule("MAJORITY")
                    .totalMembers(5)
                    .build();
        }
    }

    /**
     * Local config class for committee voting.
     */
    @Data
    @Builder
    public static class CommitteeConfig {
        private String committeeCode;
        private String decisionRule; // UNANIMOUS, MAJORITY, THRESHOLD
        private Integer totalMembers;
        private Integer requiredApprovals;
    }
}
