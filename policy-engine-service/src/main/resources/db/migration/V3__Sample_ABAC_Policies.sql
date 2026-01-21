-- Sample ABAC Policies demonstrating real-world authorization rules

-- ============================================================
-- Policy 1: Loan Officer Branch Approval
-- A loan officer can approve loans in their branch, up to their limit
-- ============================================================
INSERT INTO policies (id, name, description, resource_type, action, effect, priority, is_active, version)
VALUES (
    'a1000000-0000-0000-0000-000000000001',
    'Loan Officer Branch Approval',
    'Loan officers can approve loans in their assigned branches up to their approval limit',
    'LOAN',
    'APPROVE',
    'ALLOW',
    100,
    true,
    1
) ON CONFLICT (name) DO NOTHING;

-- Rule 1: Must have loan:approve permission
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b1000000-0000-0000-0000-000000000001',
    'a1000000-0000-0000-0000-000000000001',
    'permission_check',
    'subject.permissions',
    'CONTAINS',
    'STRING',
    'loan:approve',
    'User must have loan:approve permission',
    1,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule 2: Loan branch must be in user's assigned branches
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b1000000-0000-0000-0000-000000000002',
    'a1000000-0000-0000-0000-000000000001',
    'branch_check',
    'resource.branchId',
    'IN',
    'EXPRESSION',
    'subject.branchIds',
    'Loan must be in one of user assigned branches',
    2,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule 3: Loan amount must be within user's approval limit
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b1000000-0000-0000-0000-000000000003',
    'a1000000-0000-0000-0000-000000000001',
    'amount_check',
    'resource.amount',
    'LESS_THAN_OR_EQUAL',
    'EXPRESSION',
    'subject.approvalLimit',
    'Loan amount must be within approval limit',
    3,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule 4: Must be during business hours (9am-6pm, weekdays)
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition, time_from, time_to)
VALUES (
    'b1000000-0000-0000-0000-000000000004',
    'a1000000-0000-0000-0000-000000000001',
    'time_check',
    'subject.id',
    'IS_NOT_NULL',
    'STRING',
    '',
    'Approvals only during business hours',
    4,
    'BUSINESS_HOURS',
    '09:00',
    '18:00'
) ON CONFLICT DO NOTHING;

-- ============================================================
-- Policy 2: Loan Read Access
-- Anyone with loan:read can view loans in their branches
-- ============================================================
INSERT INTO policies (id, name, description, resource_type, action, effect, priority, is_active, version)
VALUES (
    'a2000000-0000-0000-0000-000000000001',
    'Loan Read Access',
    'Users can read loans in their assigned branches',
    'LOAN',
    'READ',
    'ALLOW',
    100,
    true,
    1
) ON CONFLICT (name) DO NOTHING;

-- Rule: Must have loan:read permission
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b2000000-0000-0000-0000-000000000001',
    'a2000000-0000-0000-0000-000000000001',
    'permission_check',
    'subject.permissions',
    'CONTAINS',
    'STRING',
    'loan:read',
    'User must have loan:read permission',
    1,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule: Loan branch must be in user's assigned branches
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b2000000-0000-0000-0000-000000000002',
    'a2000000-0000-0000-0000-000000000001',
    'branch_check',
    'resource.branchId',
    'IN',
    'EXPRESSION',
    'subject.branchIds',
    'Loan must be in user assigned branches',
    2,
    'NONE'
) ON CONFLICT DO NOTHING;

-- ============================================================
-- Policy 3: Senior Manager Override
-- Senior managers can approve any loan regardless of branch
-- ============================================================
INSERT INTO policies (id, name, description, resource_type, action, effect, priority, is_active, version)
VALUES (
    'a3000000-0000-0000-0000-000000000001',
    'Senior Manager Override',
    'Senior managers can approve any loan up to their limit',
    'LOAN',
    'APPROVE',
    'ALLOW',
    200,
    true,
    1
) ON CONFLICT (name) DO NOTHING;

-- Rule: Must have loan:approve:all permission
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b3000000-0000-0000-0000-000000000001',
    'a3000000-0000-0000-0000-000000000001',
    'permission_check',
    'subject.permissions',
    'CONTAINS',
    'STRING',
    'loan:approve:all',
    'Senior manager override permission',
    1,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule: Still limited by approval amount
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b3000000-0000-0000-0000-000000000002',
    'a3000000-0000-0000-0000-000000000001',
    'amount_check',
    'resource.amount',
    'LESS_THAN_OR_EQUAL',
    'EXPRESSION',
    'subject.approvalLimit',
    'Must be within approval limit',
    2,
    'NONE'
) ON CONFLICT DO NOTHING;

-- ============================================================
-- Policy 4: Workflow Transition
-- Users can transition workflows they are assigned to
-- ============================================================
INSERT INTO policies (id, name, description, resource_type, action, effect, priority, is_active, version)
VALUES (
    'a4000000-0000-0000-0000-000000000001',
    'Workflow Transition Permission',
    'Users can transition workflows if they have the right permission',
    'WORKFLOW',
    'TRANSITION',
    'ALLOW',
    100,
    true,
    1
) ON CONFLICT (name) DO NOTHING;

-- Rule: Must have workflow:transition permission
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b4000000-0000-0000-0000-000000000001',
    'a4000000-0000-0000-0000-000000000001',
    'permission_check',
    'subject.permissions',
    'CONTAINS',
    'STRING',
    'workflow:transition',
    'User must have workflow:transition permission',
    1,
    'NONE'
) ON CONFLICT DO NOTHING;

-- Rule: Only on weekdays
INSERT INTO policy_rules (id, policy_id, rule_group, attribute, operator, value_type, value, description, sort_order, temporal_condition)
VALUES (
    'b4000000-0000-0000-0000-000000000002',
    'a4000000-0000-0000-0000-000000000001',
    'time_check',
    'subject.id',
    'IS_NOT_NULL',
    'STRING',
    '',
    'Transitions only on weekdays',
    2,
    'WEEKDAYS_ONLY'
) ON CONFLICT DO NOTHING;
