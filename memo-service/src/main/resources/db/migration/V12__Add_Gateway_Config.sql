-- V12: Add workflow gateway configuration table for parallel/inclusive gateway completion modes
-- Supports: ALL (default), ANY (first completes), N_OF_M (N of M must complete)

CREATE TABLE workflow_gateway_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES memo_topic(id) ON DELETE CASCADE,
    gateway_id VARCHAR(255) NOT NULL,
    gateway_name VARCHAR(255),
    gateway_type VARCHAR(50) NOT NULL DEFAULT 'PARALLEL',
    completion_mode VARCHAR(50) NOT NULL DEFAULT 'ALL',
    minimum_required INTEGER DEFAULT 1,
    total_incoming_flows INTEGER,
    description TEXT,
    cancel_remaining BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: one config per gateway per topic
CREATE UNIQUE INDEX idx_gateway_config_unique 
    ON workflow_gateway_config(topic_id, gateway_id);

-- Index for looking up configs by topic
CREATE INDEX idx_gateway_config_topic 
    ON workflow_gateway_config(topic_id);

-- Comments for documentation
COMMENT ON TABLE workflow_gateway_config IS 'Stores parallel/inclusive gateway completion configurations';
COMMENT ON COLUMN workflow_gateway_config.completion_mode IS 'ALL=wait for all, ANY=first wins, N_OF_M=custom';
COMMENT ON COLUMN workflow_gateway_config.cancel_remaining IS 'For ANY/N_OF_M: cancel pending branches when condition met';
