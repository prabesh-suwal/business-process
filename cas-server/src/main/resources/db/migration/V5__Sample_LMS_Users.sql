-- Sample LMS Product, Roles, Users for ABAC testing

-- Add LMS product if not exists
INSERT INTO products (code, name, description, status)
VALUES ('LMS', 'Loan Management System', 'Core lending application', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Add LMS permissions
INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'loan:read', 'Read Loans', 'Can view loan applications'
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'loan:approve', 'Approve Loans', 'Can approve loan applications'
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'loan:approve:all', 'Approve All Loans', 'Can approve loans from any branch'
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO permissions (product_id, code, name, description)
SELECT p.id, 'loan:create', 'Create Loans', 'Can create new loan applications'
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

-- Add LOAN_OFFICER role
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'LOAN_OFFICER', 'Loan Officer', 'Can process and approve loans in assigned branches', false
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

-- Add SENIOR_MANAGER role
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'SENIOR_MANAGER', 'Senior Manager', 'Can approve loans from any branch', false
FROM products p WHERE p.code = 'LMS'
ON CONFLICT (product_id, code) DO NOTHING;

-- Assign permissions to LOAN_OFFICER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r, permissions perm, products p
WHERE r.code = 'LOAN_OFFICER' 
  AND r.product_id = p.id 
  AND perm.product_id = p.id
  AND perm.code IN ('loan:read', 'loan:approve', 'loan:create')
  AND p.code = 'LMS'
ON CONFLICT DO NOTHING;

-- Assign permissions to SENIOR_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r, permissions perm, products p
WHERE r.code = 'SENIOR_MANAGER' 
  AND r.product_id = p.id 
  AND perm.product_id = p.id
  AND perm.code IN ('loan:read', 'loan:approve', 'loan:approve:all', 'loan:create')
  AND p.code = 'LMS'
ON CONFLICT DO NOTHING;

-- Create sample Loan Officer user
INSERT INTO users (id, username, email, password_hash, first_name, last_name, status)
VALUES (
    'c1000000-0000-0000-0000-000000000001',
    'ram.sharma',
    'ram.sharma@bank.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'Ram',
    'Sharma',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

-- Create sample Senior Manager user
INSERT INTO users (id, username, email, password_hash, first_name, last_name, status)
VALUES (
    'c2000000-0000-0000-0000-000000000001',
    'sita.devi',
    'sita.devi@bank.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'Sita',
    'Devi',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

-- Assign LOAN_OFFICER role to Ram with constraints
INSERT INTO user_roles (user_id, role_id, constraints)
SELECT 
    'c1000000-0000-0000-0000-000000000001'::uuid,
    r.id,
    '{"branchIds": ["KTM-001", "KTM-002"], "maxApprovalAmount": 500000}'::jsonb
FROM roles r, products p
WHERE r.code = 'LOAN_OFFICER' AND r.product_id = p.id AND p.code = 'LMS'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Assign SENIOR_MANAGER role to Sita with higher constraints
INSERT INTO user_roles (user_id, role_id, constraints)
SELECT 
    'c2000000-0000-0000-0000-000000000001'::uuid,
    r.id,
    '{"branchIds": ["KTM-001", "KTM-002", "PKR-001", "BTR-001"], "maxApprovalAmount": 5000000}'::jsonb
FROM roles r, products p
WHERE r.code = 'SENIOR_MANAGER' AND r.product_id = p.id AND p.code = 'LMS'
ON CONFLICT (user_id, role_id) DO NOTHING;
