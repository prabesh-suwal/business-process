-- Add default_assignee_config column to memo_topic for topic-level default assignment rules
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS default_assignee_config JSONB;

-- Add comment describing the structure
COMMENT ON COLUMN memo_topic.default_assignee_config IS 'Default assignee config for steps without specific config. Structure: {rules: [{id, name, criteria: {regionIds, districtIds, branchIds, departmentIds, groupIds, roleIds, userIds}}], fallbackRoleId, completionMode}';
