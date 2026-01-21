-- V3: Complete schema updates for FormDefinition entity
-- Adds all missing columns for versioning, categorization, and audit

-- FORM TYPE
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS form_type VARCHAR(50) DEFAULT 'GENERAL';

-- LAYOUT CONFIG
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS layout_config JSONB;

-- VERSIONING
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS previous_version_id UUID;
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS published_by UUID;

-- VALIDATION
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS validation_rules JSONB;

-- AUDIT
ALTER TABLE form_definition ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255);

-- Create index for form_type
CREATE INDEX IF NOT EXISTS idx_form_type ON form_definition(form_type);

-- Add comments
COMMENT ON COLUMN form_definition.form_type IS 'GENERAL, START_FORM, TASK_FORM, APPROVAL_FORM, DOCUMENT_FORM, CUSTOMER_FORM';
COMMENT ON COLUMN form_definition.layout_config IS 'Layout configuration for multi-step/sectioned forms';
COMMENT ON COLUMN form_definition.previous_version_id IS 'Links to previous version for versioning chain';
COMMENT ON COLUMN form_definition.published_at IS 'When this version was published/activated';
COMMENT ON COLUMN form_definition.published_by IS 'User who published this version';
COMMENT ON COLUMN form_definition.validation_rules IS 'Custom validation rules in JSON format';
COMMENT ON COLUMN form_definition.created_by_name IS 'Display name of the creator';

-- =====================================
-- CREATE FORM_DRAFT TABLE
-- =====================================
CREATE TABLE IF NOT EXISTS form_draft (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES form_definition(id) ON DELETE CASCADE,
    form_version INT,
    
    -- User info
    user_id UUID NOT NULL,
    user_name VARCHAR(255),
    
    -- Form data
    form_data JSONB NOT NULL,
    completed_fields JSONB,
    
    -- Multi-step progress
    current_step INT DEFAULT 0,
    total_steps INT,
    
    -- Entity link
    linked_entity_type VARCHAR(100),
    linked_entity_id UUID,
    context VARCHAR(255),
    
    -- Save type
    is_auto_save BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_draft_form ON form_draft(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_draft_user ON form_draft(user_id);
CREATE INDEX IF NOT EXISTS idx_draft_entity ON form_draft(linked_entity_type, linked_entity_id);

COMMENT ON TABLE form_draft IS 'Stores partial/incomplete form submissions for save-and-continue functionality';
