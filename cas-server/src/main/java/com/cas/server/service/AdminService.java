package com.cas.server.service;

import com.cas.server.domain.product.Permission;
import com.cas.server.domain.product.Product;
import com.cas.server.domain.role.Role;
import com.cas.server.domain.user.User;
import com.cas.server.domain.user.UserRole;
import com.cas.server.dto.*;
import com.cas.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ==================== USER MANAGEMENT ====================

    @Transactional(readOnly = true)
    public PageDto<UserDto> listUsers(String search, Pageable pageable) {
        Page<User> page;
        if (search != null && !search.isBlank()) {
            page = userRepository.searchUsers(search, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return toPageDto(page.map(this::toUserDto));
    }

    @Transactional(readOnly = true)
    public UserDto getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return toUserDtoWithRoles(user);
    }

    @Transactional
    public UserDto createUser(UserDto.CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .userGroup(request.getUserGroup())
                .branchId(request.getBranchId())
                .departmentId(request.getDepartmentId())
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("Created user: {}", user.getUsername());
        return toUserDto(user);
    }

    @Transactional
    public UserDto updateUser(UUID id, UserDto.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getDepartment() != null)
            user.setDepartment(request.getDepartment());
        if (request.getUserGroup() != null)
            user.setUserGroup(request.getUserGroup());
        if (request.getBranchId() != null)
            user.setBranchId(request.getBranchId());
        if (request.getDepartmentId() != null)
            user.setDepartmentId(request.getDepartmentId());
        if (request.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(request.getStatus()));
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        return toUserDto(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setStatus(User.UserStatus.LOCKED);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getUsername());
    }

    @Transactional
    public UserDto assignRoles(UUID userId, List<UUID> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Remove existing roles
        userRoleRepository.deleteByUserId(userId);

        // Add new roles
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(role)
                    .assignedAt(Instant.now())
                    .build();
            userRoleRepository.save(userRole);
        }

        log.info("Assigned {} roles to user {}", roleIds.size(), user.getUsername());
        return toUserDtoWithRoles(user);
    }

    // ==================== USER ROLE MANAGEMENT WITH CONSTRAINTS
    // ====================

    @Transactional(readOnly = true)
    public List<UserRoleDto> getUserRoles(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return user.getUserRoles().stream()
                .map(this::toUserRoleDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserRoleDto assignRoleWithConstraints(UUID userId, UserRoleDto.AssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleId()));

        // Check if user already has this role
        Optional<UserRole> existing = userRoleRepository.findByUserIdAndRoleId(userId, request.getRoleId());
        if (existing.isPresent()) {
            throw new RuntimeException("User already has role: " + role.getCode());
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .constraints(request.getConstraints() != null ? request.getConstraints() : Map.of())
                .assignedAt(Instant.now())
                .build();

        userRoleRepository.save(userRole);
        log.info("Assigned role {} to user {} with constraints {}",
                role.getCode(), user.getUsername(), request.getConstraints());

        return toUserRoleDto(userRole);
    }

    @Transactional
    public UserRoleDto updateRoleConstraints(UUID userId, UUID userRoleId,
            UserRoleDto.UpdateConstraintsRequest request) {
        UserRole userRole = userRoleRepository.findById(userRoleId)
                .orElseThrow(() -> new RuntimeException("User role assignment not found: " + userRoleId));

        if (!userRole.getUser().getId().equals(userId)) {
            throw new RuntimeException("User role does not belong to user: " + userId);
        }

        userRole.setConstraints(request.getConstraints() != null ? request.getConstraints() : Map.of());
        userRoleRepository.save(userRole);

        log.info("Updated constraints for user {} role {} to {}",
                userRole.getUser().getUsername(), userRole.getRole().getCode(), request.getConstraints());

        return toUserRoleDto(userRole);
    }

    @Transactional
    public void removeRole(UUID userId, UUID userRoleId) {
        UserRole userRole = userRoleRepository.findById(userRoleId)
                .orElseThrow(() -> new RuntimeException("User role assignment not found: " + userRoleId));

        if (!userRole.getUser().getId().equals(userId)) {
            throw new RuntimeException("User role does not belong to user: " + userId);
        }

        userRoleRepository.delete(userRole);
        log.info("Removed role {} from user {}",
                userRole.getRole().getCode(), userRole.getUser().getUsername());
    }

    private UserRoleDto toUserRoleDto(UserRole ur) {
        Role role = ur.getRole();
        return UserRoleDto.builder()
                .id(ur.getId())
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .productCode(role.getProduct().getCode())
                .productName(role.getProduct().getName())
                .constraints(ur.getConstraints())
                .assignedAt(ur.getAssignedAt())
                .permissions(role.getAllPermissionCodes().stream().toList())
                .build();
    }

    // ==================== ROLE MANAGEMENT ====================

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles(String productCode) {
        List<Role> roles;
        if (productCode != null) {
            roles = roleRepository.findByProductCodeWithPermissionsAndParent(productCode);
        } else {
            roles = roleRepository.findAll();
        }
        return roles.stream().map(this::toRoleDto).collect(Collectors.toList());
    }

    @Transactional
    public RoleDto createRole(RoleDto.CreateRoleRequest request) {
        Product product = productRepository.findByCode(request.getProductCode())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductCode()));

        Role role = Role.builder()
                .product(product)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .isSystemRole(false)
                .build();

        if (request.getParentRoleId() != null) {
            Role parent = roleRepository.findById(request.getParentRoleId())
                    .orElseThrow(() -> new RuntimeException("Parent role not found"));
            role.setParentRole(parent);
        }

        role = roleRepository.save(role);
        log.info("Created role: {} for product {}", role.getCode(), product.getCode());
        return toRoleDto(role);
    }

    @Transactional
    public RoleDto updateRole(UUID id, RoleDto.UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new RuntimeException("Cannot modify system role");
        }

        if (request.getName() != null)
            role.setName(request.getName());
        if (request.getDescription() != null)
            role.setDescription(request.getDescription());
        if (request.getParentRoleId() != null) {
            Role parent = roleRepository.findById(request.getParentRoleId())
                    .orElseThrow(() -> new RuntimeException("Parent role not found"));
            role.setParentRole(parent);
        }

        role = roleRepository.save(role);
        log.info("Updated role: {}", role.getCode());
        return toRoleDto(role);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new RuntimeException("Cannot delete system role");
        }

        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getCode());
    }

    @Transactional
    public RoleDto setRolePermissions(UUID roleId, List<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        Set<Permission> permissions = new HashSet<>();
        for (UUID permId : permissionIds) {
            Permission perm = permissionRepository.findById(permId)
                    .orElseThrow(() -> new RuntimeException("Permission not found: " + permId));
            permissions.add(perm);
        }

        role.setPermissions(permissions);
        role = roleRepository.save(role);
        log.info("Set {} permissions for role {}", permissionIds.size(), role.getCode());
        return toRoleDto(role);
    }

    // ==================== PRODUCT & PERMISSION ====================

    @Transactional(readOnly = true)
    public List<ProductDto> listProducts() {
        return productRepository.findAll().stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissions(String productCode) {
        List<Permission> permissions;
        if (productCode != null) {
            Product product = productRepository.findByCode(productCode)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            permissions = permissionRepository.findByProductId(product.getId());
        } else {
            permissions = permissionRepository.findAll();
        }
        return permissions.stream().map(this::toPermissionDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(String code) {
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product not found: " + code));
        return toProductDto(product);
    }

    @Transactional
    public ProductDto createProduct(ProductDto.CreateProductRequest request) {
        if (productRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Product code already exists: " + request.getCode());
        }

        Product product = Product.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .status(Product.ProductStatus.ACTIVE)
                .build();

        product = productRepository.save(product);

        // Auto-create "*" full access permission for the product
        createFullAccessPermission(product);

        log.info("Created product: {} with full access permission", product.getCode());
        return toProductDto(product);
    }

    /**
     * Creates a "*" full access permission for a product.
     * This permission grants all access when assigned to a role.
     */
    private void createFullAccessPermission(Product product) {
        String fullAccessCode = "*";

        // Check if permission already exists
        Optional<Permission> existing = permissionRepository.findByCode(fullAccessCode);
        Permission fullAccessPerm;

        if (existing.isPresent()) {
            // Re-use existing "*" permission and add this product
            fullAccessPerm = existing.get();
            fullAccessPerm.getProducts().add(product);
        } else {
            // Create new "*" permission
            fullAccessPerm = Permission.builder()
                    .code(fullAccessCode)
                    .name("Full Access")
                    .description("Grants full access to all resources (wildcard)")
                    .category("ADMIN")
                    .build();
            fullAccessPerm.getProducts().add(product);
        }

        permissionRepository.save(fullAccessPerm);
        log.info("Created/assigned full access permission for product: {}", product.getCode());
    }

    @Transactional
    public ProductDto updateProduct(String code, ProductDto.UpdateProductRequest request) {
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product not found: " + code));

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getStatus() != null)
            product.setStatus(Product.ProductStatus.valueOf(request.getStatus()));

        product = productRepository.save(product);
        log.info("Updated product: {}", product.getCode());
        return toProductDto(product);
    }

    @Transactional
    public void deleteProduct(String code) {
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product not found: " + code));

        // Soft delete - set to INACTIVE
        product.setStatus(Product.ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Deactivated product: {}", code);
    }

    @Transactional
    public PermissionDto createPermission(String productCode, PermissionDto.CreatePermissionRequest request) {
        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));

        Permission permission = Permission.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        // Add to products set
        permission.getProducts().add(product);

        permission = permissionRepository.save(permission);
        log.info("Created permission: {} for product {}", permission.getCode(), productCode);
        return toPermissionDto(permission);
    }

    @Transactional
    public PermissionDto updatePermission(String productCode, UUID permId,
            PermissionDto.UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findById(permId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permId));

        if (request.getName() != null)
            permission.setName(request.getName());
        if (request.getDescription() != null)
            permission.setDescription(request.getDescription());

        permission = permissionRepository.save(permission);
        log.info("Updated permission: {}", permission.getCode());
        return toPermissionDto(permission);
    }

    @Transactional
    public void deletePermission(String productCode, UUID permId) {
        Permission permission = permissionRepository.findById(permId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permId));

        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", permission.getCode());
    }

    // ==================== STANDALONE PERMISSION CRUD ====================

    @Transactional(readOnly = true)
    public List<PermissionDto> listAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PermissionDto getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        return toPermissionDto(permission);
    }

    @Transactional
    public PermissionDto createStandalonePermission(PermissionDto.CreatePermissionRequest request) {
        if (permissionRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Permission code already exists: " + request.getCode());
        }

        Permission permission = Permission.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .build();

        // Assign to products if provided
        if (request.getProductCodes() != null && !request.getProductCodes().isEmpty()) {
            for (String productCode : request.getProductCodes()) {
                Product product = productRepository.findByCode(productCode)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
                permission.getProducts().add(product);
            }
        }

        permission = permissionRepository.save(permission);
        log.info("Created standalone permission: {}", permission.getCode());
        return toPermissionDto(permission);
    }

    @Transactional
    public PermissionDto updateStandalonePermission(UUID id, PermissionDto.UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));

        if (request.getName() != null)
            permission.setName(request.getName());
        if (request.getDescription() != null)
            permission.setDescription(request.getDescription());
        if (request.getCategory() != null)
            permission.setCategory(request.getCategory());

        permission = permissionRepository.save(permission);
        log.info("Updated permission: {}", permission.getCode());
        return toPermissionDto(permission);
    }

    @Transactional
    public void deleteStandalonePermission(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", permission.getCode());
    }

    @Transactional
    public PermissionDto assignPermissionToProducts(UUID permissionId, List<String> productCodes) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        for (String productCode : productCodes) {
            Product product = productRepository.findByCode(productCode)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
            permission.getProducts().add(product);
        }

        permission = permissionRepository.save(permission);
        log.info("Assigned permission {} to products: {}", permission.getCode(), productCodes);
        return toPermissionDto(permission);
    }

    @Transactional
    public PermissionDto removePermissionFromProduct(UUID permissionId, String productCode) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        permission.getProducts().removeIf(p -> p.getCode().equals(productCode));
        permission = permissionRepository.save(permission);
        log.info("Removed permission {} from product: {}", permission.getCode(), productCode);
        return toPermissionDto(permission);
    }

    // ==================== MAPPERS ====================

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
                .department(user.getDepartment())
                .userGroup(user.getUserGroup())
                .branchId(user.getBranchId())
                .departmentId(user.getDepartmentId())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private UserDto toUserDtoWithRoles(User user) {
        UserDto dto = toUserDto(user);
        dto.setRoles(user.getUserRoles().stream()
                .map(ur -> toRoleDto(ur.getRole()))
                .collect(Collectors.toList()));
        return dto;
    }

    private RoleDto toRoleDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .productCode(role.getProduct().getCode())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(Boolean.TRUE.equals(role.getIsSystemRole()))
                .parentRoleId(role.getParentRole() != null ? role.getParentRole().getId() : null)
                .createdAt(role.getCreatedAt())
                .permissions(role.getPermissions().stream()
                        .map(this::toPermissionDto)
                        .collect(Collectors.toSet()))
                .build();
    }

    private PermissionDto toPermissionDto(Permission perm) {
        return PermissionDto.builder()
                .id(perm.getId())
                .code(perm.getCode())
                .name(perm.getName())
                .description(perm.getDescription())
                .category(perm.getCategory())
                .productCodes(perm.getProducts().stream()
                        .map(Product::getCode)
                        .collect(Collectors.toList()))
                .build();
    }

    private ProductDto toProductDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .status(product.getStatus().name())
                .permissions(product.getPermissions().stream()
                        .map(this::toPermissionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private <T> PageDto<T> toPageDto(Page<T> page) {
        return PageDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
