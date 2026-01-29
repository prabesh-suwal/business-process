-- V7__Add_Condition_Config_Column.sql
-- Add condition_config column for branching/routing conditions in workflow steps

ALTER TABLE task_configuration
ADD COLUMN IF NOT EXISTS condition_config JSONB;

COMMENT ON COLUMN task_configuration.condition_config IS 'Branching/routing conditions for workflow step transitions';
