-- Add viewer_config column to memo_topic table
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS viewer_config jsonb DEFAULT '{"viewers": []}'::jsonb;

-- Add viewer_config column to workflow_step_config table
ALTER TABLE workflow_step_config ADD COLUMN IF NOT EXISTS viewer_config jsonb DEFAULT '{"viewers": []}'::jsonb;

-- Add comment to explain the column purpose
COMMENT ON COLUMN memo_topic.viewer_config IS 'Memo-wide viewer configuration: users, roles, or departments that can view this entire memo and all its tasks';
COMMENT ON COLUMN workflow_step_config.viewer_config IS 'Step-specific viewer configuration: users, roles, or departments that can view tasks for this specific step';
