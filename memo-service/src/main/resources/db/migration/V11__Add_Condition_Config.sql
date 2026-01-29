-- V11__Add_Condition_Config.sql
-- Add condition_config column for branching/routing conditions in workflow steps

ALTER TABLE workflow_step_config
ADD COLUMN IF NOT EXISTS condition_config JSONB;

COMMENT ON COLUMN workflow_step_config.condition_config IS 'Branching/routing conditions for workflow step transitions';
