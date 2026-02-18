-- Decision Table management for DMN Business Rule Tasks
CREATE TABLE decision_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID,
    product_code VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    description TEXT,
    dmn_xml TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    flowable_deployment_id VARCHAR(255),
    flowable_decision_key VARCHAR(255),
    previous_version_id UUID,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    created_by UUID,
    created_by_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_decision_product_key_version UNIQUE(product_id, key, version)
);

CREATE INDEX idx_decision_product ON decision_table(product_id);
CREATE INDEX idx_decision_status ON decision_table(status);
CREATE INDEX idx_decision_key ON decision_table(key);
