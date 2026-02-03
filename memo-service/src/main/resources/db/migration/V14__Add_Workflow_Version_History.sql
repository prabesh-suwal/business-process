-- Workflow Version History
-- Stores snapshots of deployed workflow versions so running memos continue to work

CREATE TABLE workflow_version_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES memo_topic(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    workflow_xml TEXT NOT NULL,
    workflow_template_id VARCHAR(255),  -- Flowable process definition ID (set after deployment)
    step_configs_snapshot JSONB,        -- Snapshot of WorkflowStepConfig at time of snapshot
    gateway_rules_snapshot JSONB,       -- Snapshot of GatewayDecisionRule at time of snapshot
    deployed_at TIMESTAMP,              -- When this version was deployed
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(topic_id, version)
);

-- Index for quick lookups
CREATE INDEX idx_workflow_version_history_topic ON workflow_version_history(topic_id);

-- Add workflow_version to memo table to track which version each memo uses
ALTER TABLE memo ADD COLUMN IF NOT EXISTS workflow_version INTEGER DEFAULT 1;
