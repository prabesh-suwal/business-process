-- V6: Add organization service references to users table
-- These UUIDs reference entities in the organization-service

ALTER TABLE users ADD COLUMN IF NOT EXISTS branch_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id UUID;

-- Indexes for efficient lookup
CREATE INDEX IF NOT EXISTS idx_users_branch ON users(branch_id);
CREATE INDEX IF NOT EXISTS idx_users_department ON users(department_id);

COMMENT ON COLUMN users.branch_id IS 'References branch in organization-service';
COMMENT ON COLUMN users.department_id IS 'References department in organization-service';
