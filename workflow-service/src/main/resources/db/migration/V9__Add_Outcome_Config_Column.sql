-- Add outcome_config column to task_configuration table
-- Stores action button configuration (options with labels, styles, and process variables)
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS outcome_config jsonb;
