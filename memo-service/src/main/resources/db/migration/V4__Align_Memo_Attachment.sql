ALTER TABLE memo_attachment RENAME COLUMN file_type TO content_type;
ALTER TABLE memo_attachment RENAME COLUMN file_id TO object_name;
ALTER TABLE memo_attachment RENAME COLUMN file_size TO size;
ALTER TABLE memo_attachment RENAME COLUMN uploaded_at TO created_at;

-- uploaded_by is in DB but not in Entity (yet), we can leave it nullable or remove it.
-- For now, let's keep it as is, but code won't map it. 
