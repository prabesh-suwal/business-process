-- Add workflow_version column to memo_topic for versioning support
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS workflow_version INTEGER DEFAULT 1;

-- Update existing records to have version 1
UPDATE memo_topic SET workflow_version = 1 WHERE workflow_version IS NULL;
