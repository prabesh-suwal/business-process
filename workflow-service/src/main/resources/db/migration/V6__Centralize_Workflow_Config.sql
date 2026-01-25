-- V6: Centralize workflow configuration for multi-product support
-- This enables workflow-service to manage step configs for MMS, LMS, and future products

-- Add product_code (string identifier like MMS, LMS) to process_template
-- This is easier to work with than UUID for product identification
ALTER TABLE process_template ADD COLUMN IF NOT EXISTS product_code VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_template_product_code ON process_template(product_code);

-- Update existing templates to use MMS as default product code
UPDATE process_template SET product_code = 'MMS' WHERE product_code IS NULL;

-- Enhance task_configuration with missing fields from memo-service's WorkflowStepConfig
-- These fields were previously only in memo-service but are now centralized here

-- viewer_config: Who can view tasks at this step
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS viewer_config JSONB;
-- Structure: {viewers: [{type: "ROLE"|"DEPARTMENT"|"USER", role?: "", departmentId?: "", userId?: ""}]}

-- form_config: Form behavior at this step  
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS form_config JSONB;
-- Structure: {formCode, editableFields[], mandatoryFields[], mode: "EDIT"|"READONLY"}

-- escalation_config: Multi-level escalation rules
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS escalation_config JSONB;
-- Structure: {escalations: [{level, after, action, roles[]}]}

-- Add step_order for display ordering
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS step_order INTEGER DEFAULT 0;

-- Add active flag for soft delete
ALTER TABLE task_configuration ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;

-- Comments for documentation
COMMENT ON COLUMN task_configuration.viewer_config IS 'JSON config for who can view tasks at this step';
COMMENT ON COLUMN task_configuration.form_config IS 'JSON config for form behavior (editable fields, mode)';
COMMENT ON COLUMN task_configuration.escalation_config IS 'JSON config for multi-level escalation rules';
COMMENT ON COLUMN process_template.product_code IS 'Product identifier (MMS, LMS, etc.)';
