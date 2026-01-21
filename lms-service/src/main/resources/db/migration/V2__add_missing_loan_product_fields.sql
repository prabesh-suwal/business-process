-- V2: Add missing columns to loan_product table matching the Entity definition

-- Add loan_type column
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS loan_type VARCHAR(50) DEFAULT 'UNSECURED';

-- Add application_form_version
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS application_form_version INTEGER;

-- Add document_checklist (JSONB)
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS document_checklist JSONB;

-- Add allow_topup
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS allow_topup BOOLEAN DEFAULT FALSE;

-- Add allow_renewal
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS allow_renewal BOOLEAN DEFAULT FALSE;

-- Add allow_joint_applicants
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS allow_joint_applicants BOOLEAN DEFAULT TRUE;

-- Add max_co_applicants
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS max_co_applicants INTEGER DEFAULT 3;

-- Add amendment_mode
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS amendment_mode VARCHAR(50) DEFAULT 'FRESH';

-- Add eligibility_criteria (JSONB)
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS eligibility_criteria JSONB;

-- Add created_by
ALTER TABLE loan_product ADD COLUMN IF NOT EXISTS created_by UUID;

-- Add index for loan_type
CREATE INDEX IF NOT EXISTS idx_loan_product_loan_type ON loan_product(loan_type);
