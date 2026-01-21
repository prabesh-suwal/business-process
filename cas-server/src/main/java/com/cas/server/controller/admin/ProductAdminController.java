package com.cas.server.controller.admin;

import com.cas.server.dto.PermissionDto;
import com.cas.server.dto.ProductDto;
import com.cas.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final AdminService adminService;

    // ==================== PRODUCTS ====================

    @GetMapping
    public ResponseEntity<List<ProductDto>> listProducts() {
        return ResponseEntity.ok(adminService.listProducts());
    }

    @GetMapping("/{code}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable String code) {
        return ResponseEntity.ok(adminService.getProduct(code));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto.CreateProductRequest request) {
        return ResponseEntity.ok(adminService.createProduct(request));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable String code,
            @RequestBody ProductDto.UpdateProductRequest request) {
        return ResponseEntity.ok(adminService.updateProduct(code, request));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String code) {
        adminService.deleteProduct(code);
        return ResponseEntity.noContent().build();
    }

    // ==================== PERMISSIONS ====================

    @GetMapping("/{code}/permissions")
    public ResponseEntity<List<PermissionDto>> listPermissions(@PathVariable String code) {
        return ResponseEntity.ok(adminService.listPermissions(code));
    }

    @PostMapping("/{code}/permissions")
    public ResponseEntity<PermissionDto> createPermission(
            @PathVariable String code,
            @RequestBody PermissionDto.CreatePermissionRequest request) {
        return ResponseEntity.ok(adminService.createPermission(code, request));
    }

    @PutMapping("/{code}/permissions/{permId}")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable String code,
            @PathVariable UUID permId,
            @RequestBody PermissionDto.UpdatePermissionRequest request) {
        return ResponseEntity.ok(adminService.updatePermission(code, permId, request));
    }

    @DeleteMapping("/{code}/permissions/{permId}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable String code,
            @PathVariable UUID permId) {
        adminService.deletePermission(code, permId);
        return ResponseEntity.noContent().build();
    }
}
