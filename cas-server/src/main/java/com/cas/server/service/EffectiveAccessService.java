package com.cas.server.service;

import com.cas.server.domain.product.Permission;
import com.cas.server.domain.product.Product;
import com.cas.server.domain.user.User;
import com.cas.server.domain.user.UserRole;
import com.cas.server.dto.EffectiveAccessDto;
import com.cas.server.repository.PermissionRepository;
import com.cas.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes the effective access context for a user.
 * Resolves all role assignments → permissions → groups by module.
 * Handles wildcard (*) expansion and role inheritance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EffectiveAccessService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Build the full effective access context for a user across all assigned
     * products.
     */
    @Transactional(readOnly = true)
    public EffectiveAccessDto getEffectiveAccess(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Build user profile
        EffectiveAccessDto.UserProfile profile = EffectiveAccessDto.UserProfile.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getDisplayName())
                .build();

        // Collect all products user has roles in
        Map<String, Product> productMap = new LinkedHashMap<>();
        for (UserRole ur : user.getUserRoles()) {
            Product product = ur.getRole().getProduct();
            productMap.putIfAbsent(product.getCode(), product);
        }

        // Build access per product
        List<EffectiveAccessDto.ProductAccess> productAccessList = new ArrayList<>();
        for (Map.Entry<String, Product> entry : productMap.entrySet()) {
            String productCode = entry.getKey();
            Product product = entry.getValue();

            EffectiveAccessDto.ProductAccess productAccess = buildProductAccess(user, productCode, product);
            productAccessList.add(productAccess);
        }

        return EffectiveAccessDto.builder()
                .user(profile)
                .products(productAccessList)
                .build();
    }

    /**
     * Build access for a specific product only.
     */
    @Transactional(readOnly = true)
    public EffectiveAccessDto getEffectiveAccessForProduct(UUID userId, String productCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        EffectiveAccessDto.UserProfile profile = EffectiveAccessDto.UserProfile.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getDisplayName())
                .build();

        // Find the product from user's roles
        Product product = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getProduct())
                .filter(p -> p.getCode().equals(productCode))
                .findFirst()
                .orElse(null);

        List<EffectiveAccessDto.ProductAccess> productAccessList = new ArrayList<>();
        if (product != null) {
            productAccessList.add(buildProductAccess(user, productCode, product));
        }

        return EffectiveAccessDto.builder()
                .user(profile)
                .products(productAccessList)
                .build();
    }

    /**
     * Build access for a single product: collect permissions, expand wildcards,
     * group by module.
     */
    private EffectiveAccessDto.ProductAccess buildProductAccess(User user, String productCode, Product product) {
        // Collect all permission codes for this product (including inherited via
        // parentRole)
        Set<String> permissionCodes = user.getPermissionsForProduct(productCode);

        // Handle wildcard: if user has '*', expand to all permissions for this product
        if (permissionCodes.contains("*")) {
            permissionCodes = getAllPermissionCodesForProduct(product.getId());
        }

        // Group permissions by module (category) → action
        // Convention: permission code = "module:action", category = "MODULE"
        Map<String, List<String>> moduleActions = new TreeMap<>();
        for (String code : permissionCodes) {
            if ("*".equals(code))
                continue; // Skip wildcard itself

            String[] parts = code.split(":");
            if (parts.length == 2) {
                String module = parts[0].toUpperCase();
                String action = parts[1].toUpperCase();
                moduleActions.computeIfAbsent(module, k -> new ArrayList<>()).add(action);
            } else {
                // Handle permissions without module prefix (e.g., "mms:admin")
                String module = "ADMIN";
                String action = code.toUpperCase().replace(":", "_");
                moduleActions.computeIfAbsent(module, k -> new ArrayList<>()).add(action);
            }
        }

        // Sort actions within each module
        moduleActions.values().forEach(Collections::sort);

        // Build module list
        List<EffectiveAccessDto.ModuleAccess> modules = moduleActions.entrySet().stream()
                .map(e -> EffectiveAccessDto.ModuleAccess.builder()
                        .code(e.getKey())
                        .permissions(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // Merge constraints from all role assignments
        Map<String, Object> constraints = user.getConstraintsForProduct(productCode);

        return EffectiveAccessDto.ProductAccess.builder()
                .code(productCode)
                .name(product.getName())
                .modules(modules)
                .constraints(constraints.isEmpty() ? null : constraints)
                .build();
    }

    /**
     * Get all permission codes linked to a product (used for wildcard expansion).
     */
    private Set<String> getAllPermissionCodesForProduct(UUID productId) {
        return permissionRepository.findAll().stream()
                .filter(p -> p.getProducts().stream().anyMatch(prod -> prod.getId().equals(productId)))
                .map(Permission::getCode)
                .filter(code -> !"*".equals(code))
                .collect(Collectors.toSet());
    }
}
