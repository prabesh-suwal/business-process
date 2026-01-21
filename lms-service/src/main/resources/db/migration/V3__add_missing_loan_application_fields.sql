-- V3: Add missing columns to loan_application table matching the Entity definition

-- Add application_type
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS application_type VARCHAR(50) DEFAULT 'NEW';

-- Add parent links
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS parent_loan_id UUID;
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS parent_application_id UUID;

-- Add person_id
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS person_id UUID;
CREATE INDEX IF NOT EXISTS idx_loan_application_person ON loan_application(person_id);

-- Add co_applicants (JSONB)
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS co_applicants JSONB;

-- Add topup_amount
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS topup_amount DECIMAL(15, 2);

-- Add loan_purpose
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS loan_purpose VARCHAR(255);

-- Add sub_status
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS sub_status VARCHAR(50);

-- Add workflow task fields
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS current_task_id VARCHAR(255);
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS current_task_assignee VARCHAR(255);
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS task_assigned_at TIMESTAMP;
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS task_sla_deadline TIMESTAMP;

-- Add form_version_used
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS form_version_used INTEGER;

-- Add decided_by_name
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS decided_by_name VARCHAR(255);

-- Add rejection_reason
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(255);

-- Add branch_name
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS branch_name VARCHAR(255);

-- Add created_by and names
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255);
ALTER TABLE loan_application ADD COLUMN IF NOT EXISTS submitted_by_name VARCHAR(255);
