package com.enterprise.workflow.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the voting status for a committee task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitteeVoteDTO {

    private String taskId;
    private String committeeCode;
    private int requiredApprovals;
    private int totalMembers;
    private int approvalsReceived;
    private int rejectionsReceived;
    private String status; // PENDING, APPROVED, REJECTED
    private List<VoteRecord> votes;

    /**
     * Individual vote record.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteRecord {
        private String oderId;
        private String voterId;
        private String voterName;
        private String decision; // APPROVE, REJECT, ABSTAIN
        private String comment;
        private Instant votedAt;
    }

    /**
     * Calculate remaining votes needed.
     */
    public int getRemainingVotesNeeded() {
        return Math.max(0, requiredApprovals - approvalsReceived);
    }

    /**
     * Check if voting is complete.
     */
    public boolean isVotingComplete() {
        return "APPROVED".equals(status) || "REJECTED".equals(status);
    }
}
