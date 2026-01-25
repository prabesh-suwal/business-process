package com.cas.server.controller.admin;

import com.cas.server.dto.PageDto;
import com.cas.server.dto.UserDto;
import com.cas.server.dto.UserRoleDto;
import com.cas.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<PageDto<UserDto>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageDto<UserDto> result = adminService.listUsers(search, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUser(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto.CreateUserRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody UserDto.UpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ROLE MANAGEMENT WITH CONSTRAINTS ====================

    /**
     * Get all role assignments for a user (grouped by product).
     */
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<UserRoleDto>> getUserRoles(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserRoles(id));
    }

    /**
     * Assign a role to a user with optional constraints.
     */
    @PostMapping("/{id}/roles")
    public ResponseEntity<UserRoleDto> assignRole(
            @PathVariable UUID id,
            @RequestBody UserRoleDto.AssignRoleRequest request) {
        return ResponseEntity.ok(adminService.assignRoleWithConstraints(id, request));
    }

    /**
     * Update constraints for an existing role assignment.
     */
    @PutMapping("/{userId}/roles/{userRoleId}/constraints")
    public ResponseEntity<UserRoleDto> updateRoleConstraints(
            @PathVariable UUID userId,
            @PathVariable UUID userRoleId,
            @RequestBody UserRoleDto.UpdateConstraintsRequest request) {
        return ResponseEntity.ok(adminService.updateRoleConstraints(userId, userRoleId, request));
    }

    /**
     * Remove a role from a user.
     */
    @DeleteMapping("/{userId}/roles/{userRoleId}")
    public ResponseEntity<Void> removeRole(
            @PathVariable UUID userId,
            @PathVariable UUID userRoleId) {
        adminService.removeRole(userId, userRoleId);
        return ResponseEntity.noContent().build();
    }

    // ==================== AUTHORITY-BASED USER QUERY ====================

    /**
     * Find users who have a specific role with approval authority >= the required
     * amount.
     * Used by workflow-service for authority-based task routing.
     * 
     * Example: GET
     * /admin/users/by-authority?roleName=APPROVER&requiredAmount=5000000&branchId=BR001
     */
    @GetMapping("/by-authority")
    public ResponseEntity<List<UserDto>> findUsersByAuthority(
            @RequestParam String roleName,
            @RequestParam Long requiredAmount,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String regionId,
            @RequestParam(required = false, defaultValue = "ALL") String selection) {

        List<UserDto> users;

        switch (selection.toUpperCase()) {
            case "LOWEST_MATCH":
                // Return only the user with lowest matching authority
                users = adminService.findUserWithLowestMatchingAuthority(
                        roleName, requiredAmount, branchId, regionId)
                        .map(List::of)
                        .orElse(List.of());
                break;
            case "HIGHEST":
                // Return only the user with highest authority (ignores requiredAmount)
                users = adminService.findUserWithHighestAuthority(roleName, branchId, regionId)
                        .map(List::of)
                        .orElse(List.of());
                break;
            case "ALL":
            default:
                // Return all matching users
                users = adminService.findUsersWithApprovalAuthority(
                        roleName, requiredAmount, branchId, regionId);
                break;
        }

        return ResponseEntity.ok(users);
    }
}
