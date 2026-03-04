-- ============================================================================
-- Maker-Checker Service — Seed endpoint configurations
-- ============================================================================
-- Seeds maker-checker configs for CAS Admin (users, roles) and Memo endpoints.
-- All DISABLED by default; enable via the admin UI as needed.
-- ============================================================================

-- Product IDs (from CAS products table):
--   CAS_ADMIN = 98a08c31-1eed-44c2-91e9-9f8220a5379e
--   MMS       = 88261fc4-99c9-4d2e-8292-df42786a1f65

-- ==========================
-- CAS Admin › User endpoints
-- ==========================

INSERT INTO maker_checker_config
    (product_id, service_name, endpoint_pattern, http_method, endpoint_group, description, enabled)
VALUES
    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/users', 'POST',
     'User Management', 'Create a new user', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/users/{id}', 'PUT',
     'User Management', 'Update user details', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/users/{id}', 'DELETE',
     'User Management', 'Delete a user', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/users/{id}/roles', 'POST',
     'User Management', 'Assign a role to a user', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/users/{userId}/roles/{userRoleId}', 'DELETE',
     'User Management', 'Remove a role from a user', false);


-- ==========================
-- CAS Admin › Role endpoints
-- ==========================

INSERT INTO maker_checker_config
    (product_id, service_name, endpoint_pattern, http_method, endpoint_group, description, enabled)
VALUES
    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/roles', 'POST',
     'Role Management', 'Create a new role', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/roles/{id}', 'PUT',
     'Role Management', 'Update role details', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/roles/{id}', 'DELETE',
     'Role Management', 'Delete a role', false),

    ('98a08c31-1eed-44c2-91e9-9f8220a5379e', 'cas-server', '/admin/roles/{id}/permissions', 'PUT',
     'Role Management', 'Update role permissions', false);


-- ==========================
-- Memo (MMS) endpoints
-- ==========================

INSERT INTO maker_checker_config
    (product_id, service_name, endpoint_pattern, http_method, endpoint_group, description, enabled)
VALUES
    ('88261fc4-99c9-4d2e-8292-df42786a1f65', 'memo-service', '/memos/draft', 'POST',
     'Memo Management', 'Create a draft memo', false),

    ('88261fc4-99c9-4d2e-8292-df42786a1f65', 'memo-service', '/memos/{id}', 'PUT',
     'Memo Management', 'Update a memo', false),

    ('88261fc4-99c9-4d2e-8292-df42786a1f65', 'memo-service', '/memos/{id}/submit', 'POST',
     'Memo Management', 'Submit a memo for workflow processing', false),

    ('88261fc4-99c9-4d2e-8292-df42786a1f65', 'memo-service', '/memos/{id}/status', 'PUT',
     'Memo Management', 'Change memo status', false);
