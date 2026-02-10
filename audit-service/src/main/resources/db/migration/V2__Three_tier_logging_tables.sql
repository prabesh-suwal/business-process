-- Three-tier Logging System: API Logs, Audit Logs (refactored), Activity Logs

-- ====================================
-- API LOGS (Technical/System Logs)
-- Retention: 30-90 days
-- ====================================
CREATE TABLE IF NOT EXISTS api_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(50),
    trace_id VARCHAR(50),
    service_name VARCHAR(100) NOT NULL,
    instance_id VARCHAR(100),
    environment VARCHAR(20),
    
    -- Request
    http_method VARCHAR(10),
    endpoint VARCHAR(500),
    full_path VARCHAR(2000),
    query_params JSONB,
    request_headers JSONB,
    request_body TEXT,
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    authenticated_user_id VARCHAR(100),
    user_role VARCHAR(100),
    
    -- Response
    response_status INTEGER,
    response_time_ms BIGINT,
    response_body TEXT,
    error_code VARCHAR(50),
    error_message TEXT,
    exception_class VARCHAR(200),
    stack_trace TEXT,
    
    -- Routing
    upstream_service VARCHAR(100),
    downstream_service VARCHAR(100),
    external_api_called VARCHAR(500),
    retry_count INTEGER DEFAULT 0
);

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_api_logs_timestamp ON api_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_api_logs_correlation_id ON api_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_api_logs_service_name ON api_logs(service_name);
CREATE INDEX IF NOT EXISTS idx_api_logs_response_status ON api_logs(response_status);

-- ====================================
-- ACTIVITY LOGS (User Timeline)
-- Retention: 1 year
-- ====================================
CREATE TABLE IF NOT EXISTS activity_logs (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(50),
    
    -- User info
    user_id VARCHAR(100) NOT NULL,
    username VARCHAR(255),
    user_role VARCHAR(100),
    
    -- Activity details
    activity_type VARCHAR(50) NOT NULL,
    module_name VARCHAR(100),
    entity_name VARCHAR(100),
    entity_id VARCHAR(100),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    
    -- Context
    ip_address VARCHAR(50),
    device_info VARCHAR(500),
    geo_location VARCHAR(100)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_activity_logs_timestamp ON activity_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_activity_type ON activity_logs(activity_type);
CREATE INDEX IF NOT EXISTS idx_activity_logs_module_name ON activity_logs(module_name);

-- ====================================
-- MODIFY EXISTING AUDIT_LOGS TABLE
-- Add new columns for enhanced compliance logging
-- ====================================

-- Add new columns to existing audit_logs table
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS performed_by_department VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS performed_by_branch VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS device_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS parent_entity_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS business_key VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS old_value JSONB;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS new_value JSONB;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS changed_fields TEXT[];
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS approval_level INTEGER;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Update result column to allow FAILURE value
-- (Already supports SUCCESS/FAILURE based on existing schema)

-- Add index on business_key for faster lookups
CREATE INDEX IF NOT EXISTS idx_audit_logs_business_key ON audit_logs(business_key);
