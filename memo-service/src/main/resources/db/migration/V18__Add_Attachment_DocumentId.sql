-- Add document-service integration columns, remove old storage columns
ALTER TABLE memo_attachment ADD COLUMN IF NOT EXISTS document_id UUID;
ALTER TABLE memo_attachment ADD COLUMN IF NOT EXISTS download_url VARCHAR(500);
ALTER TABLE memo_attachment DROP COLUMN IF EXISTS object_name;
