-- Add constraints to user_roles for ABAC
-- Constraints limit where/how permissions apply (branch, region, amount limits)

ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS constraints JSONB DEFAULT '{}';

-- Add index for constraint queries
CREATE INDEX IF NOT EXISTS idx_user_roles_constraints ON user_roles USING GIN (constraints);

-- Example: Set branch constraints for existing admin
-- UPDATE user_roles SET constraints = '{"branchIds": ["HQ"], "maxApprovalAmount": 10000000}' WHERE ...

COMMENT ON COLUMN user_roles.constraints IS 'ABAC constraints: branchIds, regionIds, maxApprovalAmount, etc.';
