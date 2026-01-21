package com.cas.server.controller.admin;

import com.cas.server.dto.PermissionDto;
import com.cas.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Standalone Permission CRUD controller.
 * Permissions can be assigned to multiple products.
 */
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<PermissionDto>> listAllPermissions() {
        return ResponseEntity.ok(adminService.listAllPermissions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionDto> getPermission(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getPermissionById(id));
    }

    @PostMapping
    public ResponseEntity<PermissionDto> createPermission(
            @RequestBody PermissionDto.CreatePermissionRequest request) {
        return ResponseEntity.ok(adminService.createStandalonePermission(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable UUID id,
            @RequestBody PermissionDto.UpdatePermissionRequest request) {
        return ResponseEntity.ok(adminService.updateStandalonePermission(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        adminService.deleteStandalonePermission(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Product Assignment ====================

    @PostMapping("/{id}/products")
    public ResponseEntity<PermissionDto> assignToProducts(
            @PathVariable UUID id,
            @RequestBody PermissionDto.AssignProductsRequest request) {
        return ResponseEntity.ok(adminService.assignPermissionToProducts(id, request.getProductCodes()));
    }

    @DeleteMapping("/{id}/products/{productCode}")
    public ResponseEntity<PermissionDto> removeFromProduct(
            @PathVariable UUID id,
            @PathVariable String productCode) {
        return ResponseEntity.ok(adminService.removePermissionFromProduct(id, productCode));
    }
}
