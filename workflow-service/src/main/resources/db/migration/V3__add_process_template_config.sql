-- V3: Complete schema updates for ProcessTemplate entity
-- Adds all missing columns for versioning, forms, SLA, and audit

-- VERSIONING COLUMNS
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS previous_version_id UUID;

-- FORM LINKS
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS start_form_id UUID;
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS start_form_version INT;

-- CONFIGURATION
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS default_sla_hours INT;
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS config JSONB;

-- AUDIT
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255);

-- Create index for effective dates
CREATE INDEX IF NOT EXISTS idx_template_effective ON process_template(effective_from, effective_to);

-- Add comments
COMMENT ON COLUMN process_template.effective_from IS 'When this version becomes active';
COMMENT ON COLUMN process_template.effective_to IS 'When this version expires (null = forever)';
COMMENT ON COLUMN process_template.previous_version_id IS 'Links to previous version for versioning chain';
COMMENT ON COLUMN process_template.start_form_id IS 'ID of the start form for initiating this process';
COMMENT ON COLUMN process_template.default_sla_hours IS 'Default SLA hours for the entire process';
COMMENT ON COLUMN process_template.config IS 'Additional process-level configuration (JSONB)';
COMMENT ON COLUMN process_template.created_by_name IS 'Display name of the creator';

-- =====================================
-- CREATE TASK_CONFIGURATION TABLE
-- =====================================
CREATE TABLE IF NOT EXISTS task_configuration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_template_id UUID NOT NULL REFERENCES process_template(id) ON DELETE CASCADE,
    task_key VARCHAR(255) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    task_order INT DEFAULT 0,
    
    -- Form mapping
    form_id UUID,
    form_version INT,
    
    -- Maker-checker
    requires_maker_checker BOOLEAN DEFAULT FALSE,
    checker_roles JSONB,
    
    -- SLA
    sla_hours INT,
    warning_hours INT,
    escalation_role VARCHAR(255),
    
    -- Return/rework
    can_return_to JSONB,
    
    -- Notifications
    assignment_notification_code VARCHAR(255),
    completion_notification_code VARCHAR(255),
    sla_warning_notification_code VARCHAR(255),
    sla_breach_notification_code VARCHAR(255),
    
    -- Assignment
    assignment_config JSONB,
    
    -- Additional config
    config JSONB,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_task_config_template ON task_configuration(process_template_id);
CREATE INDEX IF NOT EXISTS idx_task_config_key ON task_configuration(task_key);

COMMENT ON TABLE task_configuration IS 'Per-task settings for workflow tasks including SLA, maker-checker, and notifications';
