-- Seed data for CAS
-- Products and their permissions

-- Insert Products
INSERT INTO products (id, code, name, description) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'LMS', 'Loan Management System', 'Core loan management product'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'WFM', 'Workflow Management', 'Workflow and process management');

-- LMS Permissions
INSERT INTO permissions (product_id, code, name, description) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.read', 'Read Loans', 'View loan details'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.create', 'Create Loans', 'Create new loan applications'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.update', 'Update Loans', 'Modify loan information'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.delete', 'Delete Loans', 'Remove loan records'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.approve', 'Approve Loans', 'Approve loan applications'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.reject', 'Reject Loans', 'Reject loan applications'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'loan.disburse', 'Disburse Loans', 'Process loan disbursement'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'customer.read', 'Read Customers', 'View customer information'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'customer.create', 'Create Customers', 'Add new customers'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'customer.update', 'Update Customers', 'Modify customer details'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'report.read', 'Read Reports', 'View loan reports'),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'report.export', 'Export Reports', 'Export loan data');

-- WFM Permissions
INSERT INTO permissions (product_id, code, name, description) VALUES
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.read', 'Read Workflows', 'View workflow definitions'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.create', 'Create Workflows', 'Design new workflows'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.update', 'Update Workflows', 'Modify workflow definitions'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.delete', 'Delete Workflows', 'Remove workflows'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.execute', 'Execute Workflows', 'Start workflow instances'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'workflow.admin', 'Administer Workflows', 'Full workflow administration'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'task.read', 'Read Tasks', 'View assigned tasks'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'task.complete', 'Complete Tasks', 'Mark tasks as complete'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'task.assign', 'Assign Tasks', 'Assign tasks to users'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'process.read', 'Read Processes', 'View running processes'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'process.cancel', 'Cancel Processes', 'Terminate running processes');

-- System Roles for LMS
INSERT INTO roles (id, product_id, code, name, description, is_system_role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 
     'LMS_ADMIN', 'LMS Administrator', 'Full access to LMS', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 
     'LOAN_OFFICER', 'Loan Officer', 'Can create and process loans', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 
     'LOAN_APPROVER', 'Loan Approver', 'Can approve/reject loans', TRUE),
    ('44444444-4444-4444-4444-444444444444', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 
     'LMS_VIEWER', 'LMS Viewer', 'Read-only access to LMS', TRUE);

-- System Roles for WFM
INSERT INTO roles (id, product_id, code, name, description, is_system_role) VALUES
    ('55555555-5555-5555-5555-555555555555', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 
     'WFM_ADMIN', 'Workflow Administrator', 'Full access to workflow engine', TRUE),
    ('66666666-6666-6666-6666-666666666666', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 
     'WORKFLOW_DESIGNER', 'Workflow Designer', 'Can design and modify workflows', TRUE),
    ('77777777-7777-7777-7777-777777777777', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 
     'TASK_USER', 'Task User', 'Can view and complete assigned tasks', TRUE);

-- Role hierarchy: LOAN_APPROVER inherits from LOAN_OFFICER
UPDATE roles SET parent_role_id = '22222222-2222-2222-2222-222222222222' 
WHERE id = '33333333-3333-3333-3333-333333333333';

-- Default admin user (password: admin123)
-- BCrypt hash verified to work with admin123
INSERT INTO users (id, username, email, password_hash, first_name, last_name, status) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin', 'admin@cas.local', 
     '$2a$10$a9KLiG28nneGVRkBkk/urO9KK84hT5OLBtpuHMQG0uB2tFe7dnlYy', 
     'System', 'Administrator', 'ACTIVE');
