-- Add override permissions to memo_topic
-- Controls what users can customize when creating memos for this topic
ALTER TABLE memo_topic ADD COLUMN override_permissions jsonb;

-- Add workflow overrides to memo
-- Stores custom workflow steps when user overrides the default workflow
ALTER TABLE memo ADD COLUMN workflow_overrides jsonb;

-- Add comments
COMMENT ON COLUMN memo_topic.override_permissions IS 'JSON object with flags: {allowOverrideAssignments, allowOverrideSLA, allowOverrideEscalation, allowOverrideViewers}';
COMMENT ON COLUMN memo.workflow_overrides IS 'Custom workflow configuration when user overrides default: {customWorkflow: true, steps: [...]}';
