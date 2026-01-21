-- Workflow Service Custom Tables
-- Flowable will create its own ACT_* tables automatically

-- Process Template: Links our metadata to Flowable process definitions
CREATE TABLE process_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    flowable_process_def_key VARCHAR(255),
    flowable_deployment_id VARCHAR(255),
    version INT DEFAULT 1,
    status VARCHAR(50) DEFAULT 'DRAFT',
    bpmn_xml TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(product_id, name, version)
);

-- Variable change audit for complete history
CREATE TABLE variable_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    variable_name VARCHAR(255) NOT NULL,
    variable_type VARCHAR(100),
    old_value JSONB,
    new_value JSONB,
    changed_by UUID,
    changed_by_name VARCHAR(255),
    changed_at TIMESTAMP DEFAULT NOW(),
    change_reason TEXT
);

CREATE INDEX idx_variable_audit_process ON variable_audit(process_instance_id);
CREATE INDEX idx_variable_audit_task ON variable_audit(task_id);

-- Complete action timeline (who did what, when)
CREATE TABLE action_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_instance_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    task_id VARCHAR(64),
    task_name VARCHAR(255),
    actor_id UUID,
    actor_name VARCHAR(255),
    actor_roles TEXT[],
    metadata JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_action_timeline_process ON action_timeline(process_instance_id);
CREATE INDEX idx_action_timeline_actor ON action_timeline(actor_id);
CREATE INDEX idx_action_timeline_type ON action_timeline(action_type);

-- Form mapping per task in a process template
CREATE TABLE process_template_form (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_template_id UUID NOT NULL REFERENCES process_template(id) ON DELETE CASCADE,
    task_key VARCHAR(255) NOT NULL,
    form_definition_id UUID NOT NULL,
    form_type VARCHAR(50) DEFAULT 'TASK_FORM',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(process_template_id, task_key)
);

-- Process instance metadata (extends Flowable's data)
CREATE TABLE process_instance_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flowable_process_instance_id VARCHAR(64) NOT NULL UNIQUE,
    process_template_id UUID REFERENCES process_template(id),
    product_id UUID NOT NULL,
    business_key VARCHAR(255),
    title VARCHAR(500),
    started_by UUID,
    started_by_name VARCHAR(255),
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'RUNNING',
    priority INT DEFAULT 0,
    due_date TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_process_metadata_flowable ON process_instance_metadata(flowable_process_instance_id);
CREATE INDEX idx_process_metadata_product ON process_instance_metadata(product_id);
CREATE INDEX idx_process_metadata_status ON process_instance_metadata(status);
