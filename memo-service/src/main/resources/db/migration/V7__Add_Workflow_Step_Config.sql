-- Workflow step configuration

CREATE TABLE workflow_step_config (
    id UUID PRIMARY KEY,
    memo_topic_id UUID NOT NULL REFERENCES memo_topic(id),
    task_key VARCHAR(100) NOT NULL,
    task_name VARCHAR(255),
    step_order INTEGER,
    assignment_config JSONB,
    form_config JSONB,
    sla_config JSONB,
    escalation_config JSONB,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(memo_topic_id, task_key)
);

CREATE INDEX idx_step_config_topic ON workflow_step_config(memo_topic_id);

-- Gateway decision rules

CREATE TABLE gateway_decision_rule (
    id UUID PRIMARY KEY,
    memo_topic_id UUID NOT NULL REFERENCES memo_topic(id),
    gateway_key VARCHAR(100) NOT NULL,
    gateway_name VARCHAR(255),
    rules JSONB,
    default_flow VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    activated_at TIMESTAMP,
    activated_by UUID,
    UNIQUE(memo_topic_id, gateway_key, version)
);

CREATE INDEX idx_gateway_rule_topic ON gateway_decision_rule(memo_topic_id);
