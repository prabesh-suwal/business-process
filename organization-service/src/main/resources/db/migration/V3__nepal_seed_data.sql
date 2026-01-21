-- V3: Nepal Seed Data
-- Seed hierarchy types and some sample locations for Nepal

-- Nepal Hierarchy Types
INSERT INTO geo_hierarchy_types (id, country_code, code, name, local_name, level, parent_type_id, description) VALUES
    ('11111111-1111-1111-1111-111111111101', 'NP', 'COUNTRY', 'Country', 'देश', 1, NULL, 'Top level - Country'),
    ('11111111-1111-1111-1111-111111111102', 'NP', 'PROVINCE', 'Province', 'प्रदेश', 2, '11111111-1111-1111-1111-111111111101', 'Province/State level'),
    ('11111111-1111-1111-1111-111111111103', 'NP', 'DISTRICT', 'District', 'जिल्ला', 3, '11111111-1111-1111-1111-111111111102', 'District level'),
    ('11111111-1111-1111-1111-111111111104', 'NP', 'MUNICIPALITY', 'Municipality', 'नगरपालिका', 4, '11111111-1111-1111-1111-111111111103', 'Municipality/Rural Municipality'),
    ('11111111-1111-1111-1111-111111111105', 'NP', 'WARD', 'Ward', 'वडा', 5, '11111111-1111-1111-1111-111111111104', 'Ward level');

-- Nepal Country
INSERT INTO geo_locations (id, country_code, code, name, local_name, type_id, parent_id, full_path) VALUES
    ('22222222-2222-2222-2222-222222222201', 'NP', 'NP', 'Nepal', 'नेपाल', '11111111-1111-1111-1111-111111111101', NULL, '/NP');

-- Provinces of Nepal
INSERT INTO geo_locations (id, country_code, code, name, local_name, type_id, parent_id, full_path) VALUES
    ('22222222-2222-2222-2222-222222222202', 'NP', 'NP-P1', 'Koshi Province', 'कोशी प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/KOSHI'),
    ('22222222-2222-2222-2222-222222222203', 'NP', 'NP-P2', 'Madhesh Province', 'मधेश प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/MADHESH'),
    ('22222222-2222-2222-2222-222222222204', 'NP', 'NP-BA', 'Bagmati Province', 'बागमती प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/BAGMATI'),
    ('22222222-2222-2222-2222-222222222205', 'NP', 'NP-GA', 'Gandaki Province', 'गण्डकी प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/GANDAKI'),
    ('22222222-2222-2222-2222-222222222206', 'NP', 'NP-LU', 'Lumbini Province', 'लुम्बिनी प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/LUMBINI'),
    ('22222222-2222-2222-2222-222222222207', 'NP', 'NP-KA', 'Karnali Province', 'कर्णाली प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/KARNALI'),
    ('22222222-2222-2222-2222-222222222208', 'NP', 'NP-SU', 'Sudurpashchim Province', 'सुदूरपश्चिम प्रदेश', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201', '/NP/SUDURPASHCHIM');

-- Sample Districts (Bagmati Province)
INSERT INTO geo_locations (id, country_code, code, name, local_name, type_id, parent_id, full_path) VALUES
    ('33333333-3333-3333-3333-333333333301', 'NP', 'NP-KTM', 'Kathmandu', 'काठमाडौं', '11111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222204', '/NP/BAGMATI/KATHMANDU'),
    ('33333333-3333-3333-3333-333333333302', 'NP', 'NP-LAL', 'Lalitpur', 'ललितपुर', '11111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222204', '/NP/BAGMATI/LALITPUR'),
    ('33333333-3333-3333-3333-333333333303', 'NP', 'NP-BHK', 'Bhaktapur', 'भक्तपुर', '11111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222204', '/NP/BAGMATI/BHAKTAPUR');

-- Sample Municipalities (Kathmandu District)
INSERT INTO geo_locations (id, country_code, code, name, local_name, type_id, parent_id, full_path) VALUES
    ('44444444-4444-4444-4444-444444444401', 'NP', 'NP-KMC', 'Kathmandu Metropolitan City', 'काठमाडौं महानगरपालिका', '11111111-1111-1111-1111-111111111104', '33333333-3333-3333-3333-333333333301', '/NP/BAGMATI/KATHMANDU/KMC'),
    ('44444444-4444-4444-4444-444444444402', 'NP', 'NP-BDG', 'Budhanilkantha Municipality', 'बूढानिलकण्ठ नगरपालिका', '11111111-1111-1111-1111-111111111104', '33333333-3333-3333-3333-333333333301', '/NP/BAGMATI/KATHMANDU/BUDHANILKANTHA'),
    ('44444444-4444-4444-4444-444444444403', 'NP', 'NP-TKS', 'Tokha Municipality', 'टोखा नगरपालिका', '11111111-1111-1111-1111-111111111104', '33333333-3333-3333-3333-333333333301', '/NP/BAGMATI/KATHMANDU/TOKHA');

-- Sample Branches
INSERT INTO branches (id, code, name, local_name, branch_type, geo_location_id, status) VALUES
    ('55555555-5555-5555-5555-555555555501', 'HO-001', 'Head Office', 'प्रधान कार्यालय', 'HEAD_OFFICE', '44444444-4444-4444-4444-444444444401', 'ACTIVE'),
    ('55555555-5555-5555-5555-555555555502', 'KTM-001', 'Kathmandu Main Branch', 'काठमाडौं मुख्य शाखा', 'BRANCH', '44444444-4444-4444-4444-444444444401', 'ACTIVE'),
    ('55555555-5555-5555-5555-555555555503', 'LAL-001', 'Lalitpur Branch', 'ललितपुर शाखा', 'BRANCH', '33333333-3333-3333-3333-333333333302', 'ACTIVE');

-- Sample Departments
INSERT INTO departments (id, code, name, description, parent_id, branch_id, status) VALUES
    ('66666666-6666-6666-6666-666666666601', 'LOAN', 'Loan Department', 'All loan related operations', NULL, NULL, 'ACTIVE'),
    ('66666666-6666-6666-6666-666666666602', 'LOAN-RETAIL', 'Retail Loans', 'Personal, Home, Vehicle loans', '66666666-6666-6666-6666-666666666601', NULL, 'ACTIVE'),
    ('66666666-6666-6666-6666-666666666603', 'LOAN-CORP', 'Corporate Loans', 'Business and corporate lending', '66666666-6666-6666-6666-666666666601', NULL, 'ACTIVE'),
    ('66666666-6666-6666-6666-666666666604', 'OPS', 'Operations', 'Day-to-day banking operations', NULL, NULL, 'ACTIVE'),
    ('66666666-6666-6666-6666-666666666605', 'IT', 'Information Technology', 'IT systems and support', NULL, NULL, 'ACTIVE');

-- Sample Groups
INSERT INTO org_groups (id, code, name, description, group_type, branch_id, department_id, status) VALUES
    ('77777777-7777-7777-7777-777777777701', 'LOAN-APPROVAL-COM', 'Loan Approval Committee', 'Reviews and approves large loans', 'COMMITTEE', NULL, '66666666-6666-6666-6666-666666666601', 'ACTIVE'),
    ('77777777-7777-7777-7777-777777777702', 'IT-TEAM', 'Core IT Team', 'Development and infrastructure team', 'TEAM', NULL, '66666666-6666-6666-6666-666666666605', 'ACTIVE');
