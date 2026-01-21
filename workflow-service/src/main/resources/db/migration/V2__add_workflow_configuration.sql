-- V2: Add workflow_configuration table for linking products, workflows, forms, and assignment rules

CREATE TABLE workflow_configuration (
    id UUID PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    process_template_id UUID REFERENCES process_template(id),
    start_form_id UUID,
    task_form_mappings JSONB DEFAULT '{}',
    assignment_rules JSONB DEFAULT '{}',
    config JSONB DEFAULT '{}',
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_wf_config_product ON workflow_configuration(product_code);
CREATE INDEX idx_wf_config_active ON workflow_configuration(product_code, active);
CREATE INDEX idx_wf_config_process ON workflow_configuration(process_template_id);

COMMENT ON TABLE workflow_configuration IS 'Links products to workflows, forms, and assignment rules';
COMMENT ON COLUMN workflow_configuration.product_code IS 'Product identifier (LMS, MEMO, DMS)';
COMMENT ON COLUMN workflow_configuration.code IS 'Unique configuration code (HOME_LOAN, INTERNAL_MEMO)';
COMMENT ON COLUMN workflow_configuration.task_form_mappings IS 'JSON mapping taskKey -> formId';
COMMENT ON COLUMN workflow_configuration.assignment_rules IS 'JSON containing default and task-specific assignment rules';
COMMENT ON COLUMN workflow_configuration.config IS 'Product-specific configuration settings';
