-- CAS_ADMIN Product: Treat Admin as a proper product instead of hardcoded special case

-- Add CAS_ADMIN product
INSERT INTO products (code, name, description, status)
VALUES ('CAS_ADMIN', 'CAS Administration', 'Central Authentication Service Administration Portal', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Add CAS_ADMIN permissions
INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'users:read', 'View Users', 'Can view user list and details'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'users:write', 'Manage Users', 'Can create, update, delete users'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'roles:read', 'View Roles', 'Can view roles and permissions'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'roles:write', 'Manage Roles', 'Can create, update, delete roles'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'clients:read', 'View API Clients', 'Can view API client list'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'clients:write', 'Manage API Clients', 'Can create, update, revoke API clients'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'audit:read', 'View Audit Logs', 'Can view audit logs'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'policies:read', 'View Policies', 'Can view authorization policies'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'policies:write', 'Manage Policies', 'Can create, update, delete policies'
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

-- Add CAS_SUPER_ADMIN role with all permissions
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'CAS_SUPER_ADMIN', 'CAS Super Administrator', 'Full access to CAS administration', true
FROM products p WHERE p.code = 'CAS_ADMIN'
ON CONFLICT (product_id, code) DO NOTHING;

-- Assign all CAS_ADMIN permissions to CAS_SUPER_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN permissions perm ON perm.product_id = p.id
WHERE r.code = 'CAS_SUPER_ADMIN' 
  AND p.code = 'CAS_ADMIN'
ON CONFLICT DO NOTHING;

-- Assign CAS_SUPER_ADMIN role to existing admin user
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
JOIN products p ON r.product_id = p.id
WHERE u.username = 'admin' 
  AND r.code = 'CAS_SUPER_ADMIN'
  AND p.code = 'CAS_ADMIN'
ON CONFLICT DO NOTHING;
