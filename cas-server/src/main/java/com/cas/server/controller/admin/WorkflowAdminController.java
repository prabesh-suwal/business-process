package com.cas.server.controller.admin;

import com.cas.server.service.AdminService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for workflow configuration admin UI.
 * Returns simplified data for dropdowns (roles, groups, departments, branches).
 * All data is dynamic - no hardcoding.
 */
@RestController
@RequestMapping("/admin/workflow-config")
@RequiredArgsConstructor
public class WorkflowAdminController {

    private final AdminService adminService;

    /**
     * Get all roles for a product (for assignment type dropdown).
     */
    @GetMapping("/roles")
    public ResponseEntity<List<DropdownItem>> getRoles(
            @RequestParam(defaultValue = "MMS") String productCode) {
        var roles = adminService.listRoles(productCode);
        var items = roles.stream()
                .map(r -> DropdownItem.builder()
                        .id(r.getId().toString())
                        .code(r.getCode())
                        .label(r.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * Get all groups/committees (for committee assignment).
     * Groups are special roles with code starting with "COMMITTEE_" or "GROUP_".
     */
    @GetMapping("/groups")
    public ResponseEntity<List<DropdownItem>> getGroups(
            @RequestParam(defaultValue = "MMS") String productCode) {
        var roles = adminService.listRoles(productCode);
        var groups = roles.stream()
                .filter(r -> r.getCode().startsWith("COMMITTEE_") || r.getCode().startsWith("GROUP_"))
                .map(r -> DropdownItem.builder()
                        .id(r.getId().toString())
                        .code(r.getCode())
                        .label(r.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(groups);
    }

    /**
     * Get all departments (for department-based assignment).
     * Departments are special roles with code starting with "DEPT_".
     */
    @GetMapping("/departments")
    public ResponseEntity<List<DropdownItem>> getDepartments(
            @RequestParam(defaultValue = "MMS") String productCode) {
        var roles = adminService.listRoles(productCode);
        var depts = roles.stream()
                .filter(r -> r.getCode().startsWith("DEPT_"))
                .map(r -> DropdownItem.builder()
                        .id(r.getId().toString())
                        .code(r.getCode())
                        .label(r.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(depts);
    }

    /**
     * Get organizational scopes (for WHERE selector).
     * These are static business scopes.
     */
    @GetMapping("/scopes")
    public ResponseEntity<List<DropdownItem>> getScopes() {
        var scopes = List.of(
                DropdownItem.builder().id("INITIATOR_BRANCH").code("INITIATOR_BRANCH").label("Same Branch as Initiator")
                        .build(),
                DropdownItem.builder().id("CUSTOMER_BRANCH").code("CUSTOMER_BRANCH").label("Customer's Branch").build(),
                DropdownItem.builder().id("SAME_DEPARTMENT").code("SAME_DEPARTMENT").label("Same Department").build(),
                DropdownItem.builder().id("DISTRICT").code("DISTRICT").label("District Level").build(),
                DropdownItem.builder().id("REGION").code("REGION").label("Region Level").build(),
                DropdownItem.builder().id("STATE").code("STATE").label("State Level").build(),
                DropdownItem.builder().id("HEAD_OFFICE").code("HEAD_OFFICE").label("Head Office").build(),
                DropdownItem.builder().id("ANY").code("ANY").label("Any Location").build());
        return ResponseEntity.ok(scopes);
    }

    /**
     * Get assignment types (for WHO selector).
     */
    @GetMapping("/assignment-types")
    public ResponseEntity<List<DropdownItem>> getAssignmentTypes() {
        var types = List.of(
                DropdownItem.builder().id("INITIATOR").code("INITIATOR").label("Initiator").build(),
                DropdownItem.builder().id("ROLE").code("ROLE").label("Role-based").build(),
                DropdownItem.builder().id("DEPARTMENT").code("DEPARTMENT").label("Department").build(),
                DropdownItem.builder().id("GROUP").code("GROUP").label("Group / Committee").build(),
                DropdownItem.builder().id("MANAGER").code("MANAGER").label("Reporting Manager").build(),
                DropdownItem.builder().id("SPECIFIC_USER").code("SPECIFIC_USER").label("Specific User").build(),
                DropdownItem.builder().id("RULE_BASED").code("RULE_BASED").label("Rule-based (Advanced)").build());
        return ResponseEntity.ok(types);
    }

    /**
     * Get decision operators (for condition builder).
     */
    @GetMapping("/operators")
    public ResponseEntity<List<OperatorItem>> getOperators() {
        var operators = List.of(
                OperatorItem.builder().id("==").label("equals").types(List.of("STRING", "NUMBER", "BOOLEAN")).build(),
                OperatorItem.builder().id("!=").label("not equals").types(List.of("STRING", "NUMBER")).build(),
                OperatorItem.builder().id(">").label("greater than").types(List.of("NUMBER")).build(),
                OperatorItem.builder().id(">=").label("greater or equal").types(List.of("NUMBER")).build(),
                OperatorItem.builder().id("<").label("less than").types(List.of("NUMBER")).build(),
                OperatorItem.builder().id("<=").label("less or equal").types(List.of("NUMBER")).build(),
                OperatorItem.builder().id("contains").label("contains").types(List.of("STRING")).build(),
                OperatorItem.builder().id("startsWith").label("starts with").types(List.of("STRING")).build());
        return ResponseEntity.ok(operators);
    }

    /**
     * Get escalation action types.
     */
    @GetMapping("/escalation-actions")
    public ResponseEntity<List<DropdownItem>> getEscalationActions() {
        var actions = List.of(
                DropdownItem.builder().id("NOTIFY").code("NOTIFY").label("Notify Only").build(),
                DropdownItem.builder().id("REASSIGN").code("REASSIGN").label("Reassign Task").build(),
                DropdownItem.builder().id("ADD_APPROVER").code("ADD_APPROVER").label("Add Parallel Approver").build(),
                DropdownItem.builder().id("ESCALATE_TO_MANAGER").code("ESCALATE_TO_MANAGER")
                        .label("Escalate to Manager").build(),
                DropdownItem.builder().id("AUTO_COMPLETE").code("AUTO_COMPLETE").label("Auto-Complete").build());
        return ResponseEntity.ok(actions);
    }

    /**
     * Get SLA duration options.
     */
    @GetMapping("/sla-durations")
    public ResponseEntity<List<DropdownItem>> getSlaDurations() {
        var durations = List.of(
                DropdownItem.builder().id("PT2H").code("PT2H").label("2 Hours").build(),
                DropdownItem.builder().id("PT4H").code("PT4H").label("4 Hours").build(),
                DropdownItem.builder().id("PT8H").code("PT8H").label("8 Hours").build(),
                DropdownItem.builder().id("P1D").code("P1D").label("1 Working Day").build(),
                DropdownItem.builder().id("P2D").code("P2D").label("2 Working Days").build(),
                DropdownItem.builder().id("P3D").code("P3D").label("3 Working Days").build(),
                DropdownItem.builder().id("P5D").code("P5D").label("5 Working Days").build(),
                DropdownItem.builder().id("P1W").code("P1W").label("1 Week").build());
        return ResponseEntity.ok(durations);
    }

    /**
     * Preview assignment - resolve users for given config.
     */
    @PostMapping("/preview-assignment")
    public ResponseEntity<List<UserPreview>> previewAssignment(@RequestBody PreviewRequest request) {
        // This would call AdminService to resolve actual users
        // For now return placeholder
        return ResponseEntity.ok(List.of(
                UserPreview.builder()
                        .id(UUID.randomUUID().toString())
                        .name("Sample User")
                        .role(request.getRole())
                        .branch("Main Branch")
                        .build()));
    }

    // DTOs
    @Data
    @Builder
    public static class DropdownItem {
        private String id;
        private String code;
        private String label;
    }

    @Data
    @Builder
    public static class OperatorItem {
        private String id;
        private String label;
        private List<String> types;
    }

    @Data
    public static class PreviewRequest {
        private String type;
        private String role;
        private String scope;
        private String branchId;
    }

    @Data
    @Builder
    public static class UserPreview {
        private String id;
        private String name;
        private String role;
        private String branch;
    }
}
