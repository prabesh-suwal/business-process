-- V1__Create_audit_logs_table.sql
-- Immutable audit logs table with hash chain integrity

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sequence_number BIGINT UNIQUE NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Actor Information
    actor_id VARCHAR(100),
    actor_name VARCHAR(255),
    actor_email VARCHAR(255),
    actor_type VARCHAR(50),
    actor_roles VARCHAR(1000),
    ip_address VARCHAR(50),

    -- Action Information
    action VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,

    -- Resource Information
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),

    -- Context
    description VARCHAR(2000),
    metadata TEXT,
    result VARCHAR(20) NOT NULL,
    error_message VARCHAR(2000),

    -- Source Information
    service_name VARCHAR(100),
    product_code VARCHAR(50),
    correlation_id VARCHAR(50),

    -- Tamper Evidence - Hash Chain
    previous_hash VARCHAR(64),
    record_hash VARCHAR(64) NOT NULL
);

-- Indexes for common queries
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_correlation_id ON audit_logs(correlation_id);
CREATE INDEX idx_audit_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_category ON audit_logs(category);
CREATE INDEX idx_audit_service ON audit_logs(service_name);
CREATE INDEX idx_audit_product ON audit_logs(product_code);
CREATE INDEX idx_audit_sequence ON audit_logs(sequence_number);

-- Database-level immutability rules
-- Prevent UPDATE on this table
CREATE OR REPLACE FUNCTION prevent_audit_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be updated';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_audit_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_update();

-- Prevent DELETE on this table (only superuser can drop)
CREATE OR REPLACE FUNCTION prevent_audit_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be deleted';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_audit_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_delete();

-- Sequence for guaranteed ordering
CREATE SEQUENCE IF NOT EXISTS audit_sequence START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE audit_logs IS 'Immutable audit logs with hash chain for tamper evidence';
COMMENT ON COLUMN audit_logs.sequence_number IS 'Monotonically increasing sequence for ordering and chain verification';
COMMENT ON COLUMN audit_logs.previous_hash IS 'SHA-256 hash of the previous audit record for chain integrity';
COMMENT ON COLUMN audit_logs.record_hash IS 'SHA-256 hash of this record content for tamper detection';
