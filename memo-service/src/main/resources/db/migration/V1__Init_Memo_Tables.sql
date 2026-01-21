CREATE TABLE memo_category (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    access_policy VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE memo_topic (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES memo_category(id),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    workflow_template_id UUID,
    form_definition_id UUID,
    content_template JSONB,
    numbering_pattern VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE memo (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL REFERENCES memo_topic(id),
    memo_number VARCHAR(100) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    content JSONB,
    data JSONB,
    status VARCHAR(50) NOT NULL,
    process_instance_id VARCHAR(100),
    current_stage VARCHAR(100),
    current_assignee VARCHAR(100),
    created_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE memo_attachment (
    id UUID PRIMARY KEY,
    memo_id UUID NOT NULL REFERENCES memo(id),
    file_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by UUID,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_memo_category_code ON memo_category(code);
CREATE INDEX idx_memo_topic_code ON memo_topic(code);
CREATE INDEX idx_memo_number ON memo(memo_number);
CREATE INDEX idx_memo_status ON memo(status);
CREATE INDEX idx_memo_created_by ON memo(created_by);
