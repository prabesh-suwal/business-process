-- ============================================================================
-- V9: Seed MMS (Memo Management System) Product, Permissions, and Sample Data
-- ============================================================================
-- Permissions are STATIC (the action catalog for MMS).
-- Roles are DYNAMIC (admin creates them via CAS Admin UI).
-- Only MMS_ADMIN system role is seeded for bootstrapping.
-- Sample roles and users are for dev/testing only.
-- ============================================================================

-- ============================================================================
-- 1. MMS Product
-- ============================================================================
INSERT INTO products (code, name, description, status)
VALUES ('MMS', 'Memo Management System', 'Enterprise memo and approval workflow management', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- 2. Permission Catalog (22 permissions across 6 modules)
-- ============================================================================
-- Convention: code = 'module:action', category = 'MODULE'
-- Frontend resolves to: MMS.MODULE.ACTION

-- ---- MODULE: MEMO ----
INSERT INTO permissions (code, name, description, category)
VALUES ('memo:view', 'View Memos', 'Can view memo list and details', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:create', 'Create Memos', 'Can create new memo drafts', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:update', 'Update Memos', 'Can edit memo content', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:delete', 'Delete Memos', 'Can delete memo drafts', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:submit', 'Submit Memos', 'Can submit memos for approval workflow', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:approve', 'Approve Memos', 'Can approve memos in workflow', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:reject', 'Reject Memos', 'Can reject memos in workflow', 'MEMO')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('memo:send_back', 'Send Back Memos', 'Can send memos back to previous step', 'MEMO')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: TASK ----
INSERT INTO permissions (code, name, description, category)
VALUES ('task:view', 'View Tasks', 'Can view task inbox and task details', 'TASK')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('task:complete', 'Complete Tasks', 'Can complete assigned tasks', 'TASK')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('task:assign', 'Assign Tasks', 'Can assign or delegate tasks to others', 'TASK')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: WORKFLOW ----
INSERT INTO permissions (code, name, description, category)
VALUES ('workflow:view', 'View Workflows', 'Can view workflow definitions', 'WORKFLOW')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('workflow:design', 'Design Workflows', 'Can create and modify workflow definitions', 'WORKFLOW')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('workflow:deploy', 'Deploy Workflows', 'Can deploy workflows to the engine', 'WORKFLOW')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: DMN ----
INSERT INTO permissions (code, name, description, category)
VALUES ('dmn:view', 'View Decision Tables', 'Can view DMN decision tables', 'DMN')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('dmn:create', 'Create Decision Tables', 'Can create and edit decision tables', 'DMN')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('dmn:deploy', 'Deploy Decision Tables', 'Can deploy decision tables to the engine', 'DMN')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: REPORT ----
INSERT INTO permissions (code, name, description, category)
VALUES ('report:view', 'View Reports', 'Can view reports and analytics', 'REPORT')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('report:export', 'Export Reports', 'Can export report data', 'REPORT')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: CONFIG ----
INSERT INTO permissions (code, name, description, category)
VALUES ('config:view', 'View Configuration', 'Can view system configuration', 'CONFIG')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description, category)
VALUES ('config:manage', 'Manage Configuration', 'Can modify system configuration', 'CONFIG')
ON CONFLICT (code) DO NOTHING;

-- ---- MODULE: ADMIN ----
INSERT INTO permissions (code, name, description, category)
VALUES ('mms:admin', 'Full MMS Access', 'Full administrative access to MMS', 'ADMIN')
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- 3. Link permissions to MMS product via product_permissions junction table
-- ============================================================================
INSERT INTO product_permissions (product_id, permission_id)
SELECT p.id, perm.id
FROM products p, permissions perm
WHERE p.code = 'MMS'
  AND perm.code IN (
    'memo:view', 'memo:create', 'memo:update', 'memo:delete', 'memo:submit',
    'memo:approve', 'memo:reject', 'memo:send_back',
    'task:view', 'task:complete', 'task:assign',
    'workflow:view', 'workflow:design', 'workflow:deploy',
    'dmn:view', 'dmn:create', 'dmn:deploy',
    'report:view', 'report:export',
    'config:view', 'config:manage',
    'mms:admin'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4. System Role: MMS_ADMIN (only system role — needed for bootstrapping)
-- ============================================================================
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'MMS_ADMIN', 'MMS Administrator', 'Full administrative access to Memo Management System', true
FROM products p WHERE p.code = 'MMS'
ON CONFLICT (product_id, code) DO NOTHING;

-- Note: Wildcard '*' is handled at the application level by EffectiveAccessService.
-- MMS_ADMIN gets all explicit MMS permissions below instead.

-- Also assign all explicit MMS permissions to MMS_ADMIN (belt and suspenders)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN product_permissions pp ON pp.product_id = p.id
JOIN permissions perm ON perm.id = pp.permission_id
WHERE r.code = 'MMS_ADMIN' AND p.code = 'MMS'
ON CONFLICT DO NOTHING;

-- Assign MMS_ADMIN to existing admin user
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u
CROSS JOIN roles r
JOIN products p ON r.product_id = p.id
WHERE u.username = 'admin'
  AND r.code = 'MMS_ADMIN'
  AND p.code = 'MMS'
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5. Sample Custom Roles (for dev/testing — admin creates these in production)
-- ============================================================================

-- HR_MANAGER: Creates and manages memos, can approve, full task access
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'HR_MANAGER', 'HR Manager', 'Creates and approves memos, manages tasks', false
FROM products p WHERE p.code = 'MMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN product_permissions pp ON pp.product_id = p.id
JOIN permissions perm ON perm.id = pp.permission_id
WHERE r.code = 'HR_MANAGER' AND p.code = 'MMS'
  AND perm.code IN (
    'memo:view', 'memo:create', 'memo:update', 'memo:delete', 'memo:submit',
    'memo:approve', 'memo:reject', 'memo:send_back',
    'task:view', 'task:complete', 'task:assign',
    'report:view', 'config:view'
  )
ON CONFLICT DO NOTHING;

-- BRANCH_MANAGER: Approves memos, views reports
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'BRANCH_MANAGER', 'Branch Manager', 'Branch-level memo approver with reporting access', false
FROM products p WHERE p.code = 'MMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN product_permissions pp ON pp.product_id = p.id
JOIN permissions perm ON perm.id = pp.permission_id
WHERE r.code = 'BRANCH_MANAGER' AND p.code = 'MMS'
  AND perm.code IN (
    'memo:view', 'memo:approve', 'memo:reject', 'memo:send_back',
    'task:view', 'task:complete', 'task:assign',
    'report:view', 'report:export',
    'config:view'
  )
ON CONFLICT DO NOTHING;

-- STAFF: Regular staff — creates and submits memos
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'STAFF', 'Staff', 'Regular staff member who creates and submits memos', false
FROM products p WHERE p.code = 'MMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN product_permissions pp ON pp.product_id = p.id
JOIN permissions perm ON perm.id = pp.permission_id
WHERE r.code = 'STAFF' AND p.code = 'MMS'
  AND perm.code IN (
    'memo:view', 'memo:create', 'memo:update', 'memo:delete', 'memo:submit',
    'task:view', 'task:complete',
    'config:view'
  )
ON CONFLICT DO NOTHING;

-- AUDITOR: Read-only access with reporting
INSERT INTO roles (product_id, code, name, description, is_system_role)
SELECT p.id, 'AUDITOR', 'Auditor', 'Read-only access to memos, tasks, and reports', false
FROM products p WHERE p.code = 'MMS'
ON CONFLICT (product_id, code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, perm.id
FROM roles r
JOIN products p ON r.product_id = p.id
JOIN product_permissions pp ON pp.product_id = p.id
JOIN permissions perm ON perm.id = pp.permission_id
WHERE r.code = 'AUDITOR' AND p.code = 'MMS'
  AND perm.code IN (
    'memo:view',
    'task:view',
    'report:view', 'report:export',
    'config:view'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 6. Sample Users (for dev/testing)
-- ============================================================================
-- Password for all: 'password' (bcrypt hash)

INSERT INTO users (id, username, email, password_hash, first_name, last_name, status)
VALUES (
    'd1000000-0000-0000-0000-000000000001',
    'hari.prasad',
    'hari.prasad@bank.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Hari',
    'Prasad',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, email, password_hash, first_name, last_name, status)
VALUES (
    'd2000000-0000-0000-0000-000000000001',
    'gita.kumari',
    'gita.kumari@bank.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Gita',
    'Kumari',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, email, password_hash, first_name, last_name, status)
VALUES (
    'd3000000-0000-0000-0000-000000000001',
    'binod.kumar',
    'binod.kumar@bank.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Binod',
    'Kumar',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

-- ============================================================================
-- 7. Sample User-Role Assignments (with ABAC constraints)
-- ============================================================================

-- hari.prasad → STAFF (branch-restricted)
INSERT INTO user_roles (user_id, role_id, constraints)
SELECT
    'd1000000-0000-0000-0000-000000000001'::uuid,
    r.id,
    '{"branchIds": ["KTM-001"]}'::jsonb
FROM roles r
JOIN products p ON r.product_id = p.id
WHERE r.code = 'STAFF' AND p.code = 'MMS'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- gita.kumari → BRANCH_MANAGER (multi-branch)
INSERT INTO user_roles (user_id, role_id, constraints)
SELECT
    'd2000000-0000-0000-0000-000000000001'::uuid,
    r.id,
    '{"branchIds": ["KTM-001", "KTM-002"]}'::jsonb
FROM roles r
JOIN products p ON r.product_id = p.id
WHERE r.code = 'BRANCH_MANAGER' AND p.code = 'MMS'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- binod.kumar → AUDITOR (no constraints)
INSERT INTO user_roles (user_id, role_id)
SELECT
    'd3000000-0000-0000-0000-000000000001'::uuid,
    r.id
FROM roles r
JOIN products p ON r.product_id = p.id
WHERE r.code = 'AUDITOR' AND p.code = 'MMS'
ON CONFLICT (user_id, role_id) DO NOTHING;
