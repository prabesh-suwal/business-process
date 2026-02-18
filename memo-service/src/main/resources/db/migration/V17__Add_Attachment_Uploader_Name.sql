-- Add uploader name column to memo_attachment
ALTER TABLE memo_attachment ADD COLUMN IF NOT EXISTS uploaded_by_name VARCHAR(255);
