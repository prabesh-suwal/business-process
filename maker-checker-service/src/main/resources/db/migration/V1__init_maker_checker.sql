-- ============================================================================
-- Maker-Checker Service — Initial Schema
-- ============================================================================

CREATE TABLE maker_checker_config (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id           UUID          NOT NULL,
    service_name         VARCHAR(100)  NOT NULL,
    endpoint_pattern     VARCHAR(255)  NOT NULL,
    http_method          VARCHAR(10)   NOT NULL,
    endpoint_group       VARCHAR(100),
    description          VARCHAR(500),
    same_maker_can_check BOOLEAN       NOT NULL DEFAULT FALSE,
    enabled              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by           VARCHAR(100),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, service_name, endpoint_pattern, http_method)
);

CREATE INDEX idx_config_product ON maker_checker_config(product_id);

CREATE TABLE config_checker (
    config_id  UUID NOT NULL REFERENCES maker_checker_config(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    PRIMARY KEY (config_id, user_id)
);

CREATE INDEX idx_config_checker_config ON config_checker(config_id);

CREATE TABLE approval_request (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id          UUID          NOT NULL REFERENCES maker_checker_config(id),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    http_method        VARCHAR(10)   NOT NULL,
    request_path       VARCHAR(500)  NOT NULL,
    request_body       TEXT,
    request_headers    JSONB,
    query_params       TEXT,
    maker_user_id      VARCHAR(100)  NOT NULL,
    maker_user_name    VARCHAR(200),
    maker_roles        TEXT,
    maker_product_code VARCHAR(50),
    checker_user_id    VARCHAR(100),
    checker_user_name  VARCHAR(200),
    checker_comment    TEXT,
    response_status    INTEGER,
    response_body      TEXT,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    expires_at         TIMESTAMPTZ,
    resolved_at        TIMESTAMPTZ
);

CREATE INDEX idx_approval_request_status ON approval_request(status);
CREATE INDEX idx_approval_request_maker  ON approval_request(maker_user_id);
CREATE INDEX idx_approval_request_config ON approval_request(config_id);

CREATE TABLE approval_audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_id    UUID          NOT NULL REFERENCES approval_request(id),
    action         VARCHAR(30)   NOT NULL,
    performed_by   VARCHAR(100)  NOT NULL,
    performed_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    details        TEXT,
    ip_address     VARCHAR(50)
);

CREATE INDEX idx_audit_log_approval ON approval_audit_log(approval_id);

CREATE TABLE sla_escalation_config (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id        UUID          NOT NULL REFERENCES maker_checker_config(id),
    deadline_hours   INTEGER       NOT NULL DEFAULT 24,
    escalation_role  VARCHAR(100),
    auto_expire      BOOLEAN       NOT NULL DEFAULT TRUE,
    UNIQUE(config_id)
);
