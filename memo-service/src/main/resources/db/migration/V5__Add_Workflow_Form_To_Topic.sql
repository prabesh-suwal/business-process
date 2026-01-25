-- V5: Add workflow and form schema columns to memo_topic
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS workflow_xml TEXT;
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS form_schema JSONB;
