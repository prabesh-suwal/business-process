CREATE TABLE workflow_variables (
    id UUID PRIMARY KEY,
    variable_key VARCHAR(100) NOT NULL,
    variable_value VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_workflow_variable_key UNIQUE (variable_key)
);

-- Seed initial configuration
INSERT INTO workflow_variables (id, variable_key, variable_value, label, type, description, created_at, updated_at) VALUES
(gen_random_uuid(), 'roleRM', 'RELATIONSHIP_MANAGER', 'Relationship Manager', 'ROLE', 'Role for initial review', NOW(), NOW()),
(gen_random_uuid(), 'roleBM', 'BRANCH_MANAGER', 'Branch Manager', 'ROLE', 'Role for branch verification', NOW(), NOW()),
(gen_random_uuid(), 'roleCreditAnalyst', 'CREDIT_ANALYST', 'Credit Analyst', 'ROLE', 'Role for credit analysis', NOW(), NOW()),
(gen_random_uuid(), 'roleRiskOfficer', 'RISK_OFFICER', 'Risk Officer', 'ROLE', 'Role for risk review', NOW(), NOW()),
(gen_random_uuid(), 'roleApprover', 'APPROVER', 'Approval Authority', 'ROLE', 'Role for final approval based on limits', NOW(), NOW()),
(gen_random_uuid(), 'roleDistrictHead', 'DISTRICT_HEAD', 'District Head', 'ROLE', 'Role for district level escalation', NOW(), NOW()),
(gen_random_uuid(), 'roleStateHead', 'STATE_HEAD', 'State Head', 'ROLE', 'Role for state level escalation', NOW(), NOW()),
(gen_random_uuid(), 'roleCreditCommitteeA', 'CREDIT_COMMITTEE_A', 'Credit Committee A Member', 'ROLE', 'Role for committee A members', NOW(), NOW()),
(gen_random_uuid(), 'committeeCreditA', 'CC_A', 'Credit Committee A', 'COMMITTEE', 'Up to 50L', NOW(), NOW()),
(gen_random_uuid(), 'approvalThreshold', '5000000', 'Approval Threshold (High Value)', 'NUMBER', 'Threshold for committee approval', NOW(), NOW()),
(gen_random_uuid(), 'escalationDelayLow', 'P1D', 'Escalation Delay (Low)', 'DURATION', '1 Day delay', NOW(), NOW()),
(gen_random_uuid(), 'escalationDelayMedium', 'P2D', 'Escalation Delay (Medium)', 'DURATION', '2 Days delay', NOW(), NOW()),
(gen_random_uuid(), 'escalationDelayHigh', 'P3D', 'Escalation Delay (High)', 'DURATION', '3 Days delay', NOW(), NOW());
