package com.enterprise.workflow.controller;

import com.enterprise.workflow.dto.TimelineEventDTO;
import com.enterprise.workflow.entity.ActionTimeline;
import com.enterprise.workflow.entity.VariableAudit;
import com.enterprise.workflow.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for workflow history and audit.
 * Used for viewing process timelines and variable history.
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * Get complete timeline for a process instance.
     */
    @GetMapping("/timeline/{processInstanceId}")
    public ResponseEntity<List<TimelineEventDTO>> getTimeline(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(historyService.getProcessTimeline(processInstanceId));
    }

    /**
     * Get paginated timeline for a process instance.
     */
    @GetMapping("/timeline/{processInstanceId}/paged")
    public ResponseEntity<Page<TimelineEventDTO>> getTimelinePaged(
            @PathVariable String processInstanceId,
            @PageableDefault(size = 50) Pageable pageable) {

        return ResponseEntity.ok(historyService.getProcessTimelinePaged(processInstanceId, pageable));
    }

    /**
     * Get timeline filtered by action type.
     */
    @GetMapping("/timeline/{processInstanceId}/by-type")
    public ResponseEntity<List<TimelineEventDTO>> getTimelineByType(
            @PathVariable String processInstanceId,
            @RequestParam ActionTimeline.ActionType actionType) {

        return ResponseEntity.ok(historyService.getTimelineByActionType(processInstanceId, actionType));
    }

    /**
     * Get variable history for a process instance.
     */
    @GetMapping("/variables/{processInstanceId}")
    public ResponseEntity<List<VariableAudit>> getVariableHistory(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(historyService.getVariableHistory(processInstanceId));
    }

    /**
     * Get history for a specific variable.
     */
    @GetMapping("/variables/{processInstanceId}/{variableName}")
    public ResponseEntity<List<VariableAudit>> getSpecificVariableHistory(
            @PathVariable String processInstanceId,
            @PathVariable String variableName) {

        return ResponseEntity.ok(historyService.getVariableHistory(processInstanceId, variableName));
    }
}
