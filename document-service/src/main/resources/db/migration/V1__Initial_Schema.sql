-- Initial schema for document service
CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    storage_path VARCHAR(1000) NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    linked_entity_type VARCHAR(50),
    linked_entity_id UUID,
    document_type VARCHAR(50),
    description TEXT,
    uploaded_by UUID,
    uploaded_by_name VARCHAR(255),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_document_entity ON document (linked_entity_type, linked_entity_id);
CREATE INDEX IF NOT EXISTS idx_document_uploaded_by ON document (uploaded_by);
