package com.enterprise.workflow.controller;

import com.enterprise.workflow.entity.WorkflowVariable;
import com.enterprise.workflow.service.WorkflowVariableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-variables")
@RequiredArgsConstructor
public class WorkflowVariableController {

    private final WorkflowVariableService variableService;

    @GetMapping
    public ResponseEntity<List<WorkflowVariable>> getAllVariables() {
        return ResponseEntity.ok(variableService.getAllVariables());
    }

    @PostMapping
    public ResponseEntity<WorkflowVariable> createVariable(@RequestBody WorkflowVariable variable) {
        return ResponseEntity.ok(variableService.createOrUpdate(variable));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteVariable(@PathVariable String key) {
        variableService.deleteVariable(key);
        return ResponseEntity.ok().build();
    }
}
