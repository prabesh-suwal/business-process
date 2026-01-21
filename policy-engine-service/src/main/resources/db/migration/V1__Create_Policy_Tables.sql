-- ==============================================================================
-- POLICY ENGINE SERVICE - DATABASE SCHEMA
-- ==============================================================================

-- Policies table
CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    resource_type VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    effect VARCHAR(10) NOT NULL DEFAULT 'ALLOW',
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    version INTEGER DEFAULT 1,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_effect CHECK (effect IN ('ALLOW', 'DENY'))
);

-- Policy rules (conditions that must be met)
CREATE TABLE policy_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    rule_group VARCHAR(50) DEFAULT 'default',
    attribute VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    value TEXT NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_operator CHECK (operator IN (
        'EQUALS', 'NOT_EQUALS', 'IN', 'NOT_IN', 'CONTAINS', 'CONTAINS_ANY',
        'GREATER_THAN', 'GREATER_THAN_OR_EQUAL', 'LESS_THAN', 'LESS_THAN_OR_EQUAL',
        'STARTS_WITH', 'ENDS_WITH', 'MATCHES_REGEX',
        'IS_NULL', 'IS_NOT_NULL', 'IS_TRUE', 'IS_FALSE'
    )),
    CONSTRAINT check_value_type CHECK (value_type IN (
        'STRING', 'NUMBER', 'BOOLEAN', 'ARRAY', 'EXPRESSION'
    ))
);

-- Rule groups (for complex AND/OR logic)
CREATE TABLE policy_rule_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    logic_operator VARCHAR(5) DEFAULT 'AND',
    parent_group_id UUID REFERENCES policy_rule_groups(id),
    sort_order INTEGER DEFAULT 0,
    
    CONSTRAINT check_logic CHECK (logic_operator IN ('AND', 'OR'))
);

-- Policy versions (audit trail)
CREATE TABLE policy_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    policy_snapshot JSONB NOT NULL,
    changed_by UUID,
    change_reason TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Evaluation audit logs
CREATE TABLE evaluation_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID,
    policy_name VARCHAR(100),
    subject_id VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    decision VARCHAR(10) NOT NULL,
    reason TEXT,
    evaluation_time_ms INTEGER,
    request_context JSONB,
    evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_decision CHECK (decision IN ('ALLOW', 'DENY'))
);

-- Indexes for performance
CREATE INDEX idx_policies_resource_action ON policies(resource_type, action) WHERE is_active = true;
CREATE INDEX idx_policies_active ON policies(is_active) WHERE is_active = true;
CREATE INDEX idx_policy_rules_policy_id ON policy_rules(policy_id);
CREATE INDEX idx_policy_rules_group ON policy_rules(policy_id, rule_group);
CREATE INDEX idx_policy_versions_policy ON policy_versions(policy_id, version);
CREATE INDEX idx_evaluation_logs_subject ON evaluation_audit_logs(subject_id, evaluated_at);
CREATE INDEX idx_evaluation_logs_policy ON evaluation_audit_logs(policy_id, evaluated_at);

-- Insert some default policies
INSERT INTO policies (id, name, description, resource_type, action, effect, priority, is_active, created_by)
VALUES 
    ('00000001-0001-0001-0001-000000000001', 
     'loan_branch_approval', 
     'Users can only approve loans in their assigned branches within their approval limit',
     'loan', 'approve', 'ALLOW', 100, true, NULL),
    
    ('00000001-0001-0001-0001-000000000002', 
     'loan_branch_view', 
     'Users can view loans in their assigned branches',
     'loan', 'view', 'ALLOW', 100, true, NULL),
     
    ('00000001-0001-0001-0001-000000000003', 
     'super_admin_all', 
     'Super admins have full access to everything',
     '*', '*', 'ALLOW', 1000, true, NULL);

-- Rules for loan_branch_approval
INSERT INTO policy_rules (policy_id, rule_group, attribute, operator, value_type, value, description, sort_order)
VALUES 
    -- Permission check
    ('00000001-0001-0001-0001-000000000001', 'permission', 
     'subject.permissions', 'CONTAINS', 'STRING', 'loan:approve',
     'User must have loan:approve permission', 1),
    -- Branch scope check
    ('00000001-0001-0001-0001-000000000001', 'scope', 
     'resource.branchId', 'IN', 'EXPRESSION', 'subject.branchIds',
     'Loan branch must be in user assigned branches', 2),
    -- Amount limit check
    ('00000001-0001-0001-0001-000000000001', 'limit', 
     'resource.amount', 'LESS_THAN_OR_EQUAL', 'EXPRESSION', 'subject.approvalLimit',
     'Loan amount must not exceed user approval limit', 3);

-- Rules for loan_branch_view
INSERT INTO policy_rules (policy_id, rule_group, attribute, operator, value_type, value, description, sort_order)
VALUES 
    ('00000001-0001-0001-0001-000000000002', 'permission', 
     'subject.permissions', 'CONTAINS', 'STRING', 'loan:view',
     'User must have loan:view permission', 1),
    ('00000001-0001-0001-0001-000000000002', 'scope', 
     'resource.branchId', 'IN', 'EXPRESSION', 'subject.branchIds',
     'Loan branch must be in user assigned branches', 2);

-- Rules for super_admin_all
INSERT INTO policy_rules (policy_id, rule_group, attribute, operator, value_type, value, description, sort_order)
VALUES 
    ('00000001-0001-0001-0001-000000000003', 'permission', 
     'subject.permissions', 'CONTAINS', 'STRING', '*:*',
     'User must have super admin permission', 1);

-- Rule groups
INSERT INTO policy_rule_groups (policy_id, name, logic_operator, sort_order)
VALUES 
    ('00000001-0001-0001-0001-000000000001', 'permission', 'AND', 1),
    ('00000001-0001-0001-0001-000000000001', 'scope', 'AND', 2),
    ('00000001-0001-0001-0001-000000000001', 'limit', 'AND', 3),
    ('00000001-0001-0001-0001-000000000002', 'permission', 'AND', 1),
    ('00000001-0001-0001-0001-000000000002', 'scope', 'AND', 2),
    ('00000001-0001-0001-0001-000000000003', 'permission', 'AND', 1);
