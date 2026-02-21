-- Add outcome_config column to workflow_step_config table
-- Stores action button configuration (options with labels, styles, and process variables)
ALTER TABLE workflow_step_config ADD COLUMN IF NOT EXISTS outcome_config jsonb;
