-- Add new memo domain tables for task tracking

-- MemoTask: Tracks workflow tasks in memo context
CREATE TABLE memo_task (
    id UUID PRIMARY KEY,
    memo_id UUID NOT NULL REFERENCES memo(id),
    workflow_task_id VARCHAR(255) NOT NULL,
    task_definition_key VARCHAR(255),
    task_name VARCHAR(255),
    stage VARCHAR(255),
    assigned_to VARCHAR(255),
    assigned_to_name VARCHAR(255),
    candidate_groups TEXT,
    candidate_users TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    action_taken VARCHAR(50),
    comments TEXT,
    due_date TIMESTAMP,
    priority INTEGER DEFAULT 50,
    created_at TIMESTAMP DEFAULT NOW(),
    claimed_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_memo_task_memo ON memo_task(memo_id);
CREATE INDEX idx_memo_task_workflow ON memo_task(workflow_task_id);
CREATE INDEX idx_memo_task_assignee ON memo_task(assigned_to);
CREATE INDEX idx_memo_task_status ON memo_task(status);

-- MemoComment: Comments on memo or tasks
CREATE TABLE memo_comment (
    id UUID PRIMARY KEY,
    memo_id UUID NOT NULL REFERENCES memo(id),
    task_id UUID REFERENCES memo_task(id),
    user_id UUID NOT NULL,
    user_name VARCHAR(255),
    content TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'COMMENT',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_memo_comment_memo ON memo_comment(memo_id);
CREATE INDEX idx_memo_comment_task ON memo_comment(task_id);

-- MemoVersion: Audit trail snapshots
CREATE TABLE memo_version (
    id UUID PRIMARY KEY,
    memo_id UUID NOT NULL REFERENCES memo(id),
    version INTEGER NOT NULL,
    data_snapshot JSONB,
    status_at_snapshot VARCHAR(50),
    stage_at_snapshot VARCHAR(255),
    created_by UUID,
    created_by_name VARCHAR(255),
    action VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_memo_version_memo ON memo_version(memo_id);

-- Add new columns to memo_topic
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS assignment_rules JSONB;
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS sla_rules JSONB;
ALTER TABLE memo_topic ADD COLUMN IF NOT EXISTS escalation_rules JSONB;
