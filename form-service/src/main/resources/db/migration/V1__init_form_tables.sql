-- Form Service Tables

-- Form Definition (JSON Schema based)
CREATE TABLE form_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT DEFAULT 1,
    schema JSONB NOT NULL,
    ui_schema JSONB,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(product_id, name, version)
);

CREATE INDEX idx_form_def_product ON form_definition(product_id);
CREATE INDEX idx_form_def_status ON form_definition(status);

-- Field Definitions (denormalized from schema for efficient querying)
CREATE TABLE field_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES form_definition(id) ON DELETE CASCADE,
    field_key VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    label VARCHAR(255),
    placeholder VARCHAR(255),
    help_text TEXT,
    required BOOLEAN DEFAULT FALSE,
    validation_rules JSONB,
    visibility_rules JSONB,
    options JSONB,
    default_value JSONB,
    display_order INT DEFAULT 0,
    group_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(form_definition_id, field_key)
);

CREATE INDEX idx_field_def_form ON field_definition(form_definition_id);

-- Form Submissions
CREATE TABLE form_submission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES form_definition(id),
    process_instance_id VARCHAR(64),
    task_id VARCHAR(64),
    data JSONB NOT NULL,
    submitted_by UUID,
    submitted_by_name VARCHAR(255),
    submitted_at TIMESTAMP DEFAULT NOW(),
    validation_status VARCHAR(50) DEFAULT 'VALID',
    validation_errors JSONB
);

CREATE INDEX idx_submission_form ON form_submission(form_definition_id);
CREATE INDEX idx_submission_process ON form_submission(process_instance_id);
CREATE INDEX idx_submission_task ON form_submission(task_id);

-- File uploads metadata
CREATE TABLE file_upload (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_submission_id UUID REFERENCES form_submission(id) ON DELETE CASCADE,
    field_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    minio_bucket VARCHAR(255),
    minio_object_key VARCHAR(500),
    uploaded_by UUID,
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_file_upload_submission ON file_upload(form_submission_id);
