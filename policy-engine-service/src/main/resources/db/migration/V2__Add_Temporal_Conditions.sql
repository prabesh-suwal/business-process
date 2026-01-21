-- Add temporal condition columns for time-based access control

ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS temporal_condition VARCHAR(30) DEFAULT 'NONE';
ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS time_from TIME;
ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS time_to TIME;
ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS valid_from DATE;
ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS valid_until DATE;
ALTER TABLE policy_rules ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);

-- Index for temporal queries
CREATE INDEX IF NOT EXISTS idx_policy_rules_temporal ON policy_rules (temporal_condition) WHERE temporal_condition != 'NONE';

COMMENT ON COLUMN policy_rules.temporal_condition IS 'Time-based condition: NONE, BUSINESS_HOURS, WEEKDAYS_ONLY, TIME_WINDOW, WITHIN_PERIOD';
COMMENT ON COLUMN policy_rules.time_from IS 'Start time for TIME_WINDOW/BUSINESS_HOURS (e.g., 09:00)';
COMMENT ON COLUMN policy_rules.time_to IS 'End time for TIME_WINDOW/BUSINESS_HOURS (e.g., 18:00)';
COMMENT ON COLUMN policy_rules.valid_from IS 'Start date for WITHIN_PERIOD';
COMMENT ON COLUMN policy_rules.valid_until IS 'End date for WITHIN_PERIOD';
