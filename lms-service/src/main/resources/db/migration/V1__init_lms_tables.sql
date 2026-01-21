-- V1: Initialize LMS tables

-- Loan Products
CREATE TABLE loan_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    min_amount DECIMAL(15, 2) NOT NULL,
    max_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    min_tenure INTEGER,
    max_tenure INTEGER,
    processing_fee_percent DECIMAL(5, 2) DEFAULT 0,
    workflow_template_id UUID,
    application_form_id UUID,
    product_id UUID,
    active BOOLEAN DEFAULT TRUE,
    config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Loan Applications
CREATE TABLE loan_application (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_number VARCHAR(50) NOT NULL UNIQUE,
    loan_product_id UUID NOT NULL REFERENCES loan_product(id),
    customer_id UUID,
    applicant_name VARCHAR(255),
    applicant_email VARCHAR(255),
    applicant_phone VARCHAR(50),
    requested_amount DECIMAL(15, 2) NOT NULL,
    approved_amount DECIMAL(15, 2),
    interest_rate DECIMAL(5, 2),
    requested_tenure INTEGER,
    approved_tenure INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    process_instance_id VARCHAR(255),
    current_task_name VARCHAR(255),
    application_data JSONB,
    branch_id UUID,
    submitted_by UUID,
    submitted_at TIMESTAMP,
    decided_by UUID,
    decided_at TIMESTAMP,
    decision_comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_loan_application_status ON loan_application(status);
CREATE INDEX idx_loan_application_customer ON loan_application(customer_id);
CREATE INDEX idx_loan_application_branch ON loan_application(branch_id);
CREATE INDEX idx_loan_application_submitted_by ON loan_application(submitted_by);
CREATE INDEX idx_loan_application_process ON loan_application(process_instance_id);

-- Insert sample loan products
INSERT INTO loan_product (code, name, description, min_amount, max_amount, interest_rate, min_tenure, max_tenure, processing_fee_percent)
VALUES 
    ('HOME_LOAN', 'Home Loan', 'Loan for purchasing or constructing a home', 500000, 50000000, 8.50, 12, 360, 0.50),
    ('VEHICLE_LOAN', 'Vehicle Loan', 'Loan for purchasing a vehicle', 100000, 5000000, 10.50, 12, 84, 1.00),
    ('PERSONAL_LOAN', 'Personal Loan', 'Unsecured personal loan', 50000, 2000000, 14.00, 6, 60, 2.00),
    ('EDUCATION_LOAN', 'Education Loan', 'Loan for higher education', 100000, 10000000, 9.00, 12, 180, 0.25);

COMMENT ON TABLE loan_product IS 'Defines available loan types with their parameters';
COMMENT ON TABLE loan_application IS 'Individual loan applications with full lifecycle tracking';
