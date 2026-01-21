package com.cas.server.controller.admin;

import com.cas.server.dto.PermissionDto;
import com.cas.server.dto.RoleDto;
import com.cas.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<RoleDto>> listRoles(
            @RequestParam(required = false) String productCode) {
        return ResponseEntity.ok(adminService.listRoles(productCode));
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@RequestBody RoleDto.CreateRoleRequest request) {
        return ResponseEntity.ok(adminService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable UUID id,
            @RequestBody RoleDto.UpdateRoleRequest request) {
        return ResponseEntity.ok(adminService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        adminService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleDto> setPermissions(
            @PathVariable UUID id,
            @RequestBody RoleDto.SetPermissionsRequest request) {
        return ResponseEntity.ok(adminService.setRolePermissions(id, request.getPermissionIds()));
    }
}
