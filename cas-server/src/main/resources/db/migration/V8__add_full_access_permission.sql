-- V8: Add full access (*) permission to all existing products
-- This migration creates a "*" permission that grants full access when assigned to a role

-- Create the "*" full access permission if it doesn't exist
INSERT INTO permissions (id, code, name, description, category, created_at)
SELECT 
    gen_random_uuid(),
    '*',
    'Full Access',
    'Grants full access to all resources (wildcard permission)',
    'ADMIN',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = '*'
);

-- Link the "*" permission to all existing products
INSERT INTO product_permissions (product_id, permission_id)
SELECT p.id, perm.id
FROM products p
CROSS JOIN permissions perm
WHERE perm.code = '*'
AND NOT EXISTS (
    SELECT 1 FROM product_permissions pp 
    WHERE pp.product_id = p.id AND pp.permission_id = perm.id
);

-- Also assign the "*" permission to all existing admin roles (roles that have "ADMIN" in their code)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
CROSS JOIN permissions perm
WHERE perm.code = '*'
AND r.code LIKE '%ADMIN%'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = perm.id
);
