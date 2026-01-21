-- V2: Organization Tables (Branches, Departments, Groups)

-- Branches (physical locations)
CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    local_name VARCHAR(150),
    branch_type VARCHAR(30) NOT NULL DEFAULT 'BRANCH',
    geo_location_id UUID REFERENCES geo_locations(id),
    parent_branch_id UUID REFERENCES branches(id),
    address VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Departments (functional units)
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES departments(id),
    branch_id UUID REFERENCES branches(id),
    head_user_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Groups (teams, committees)
CREATE TABLE org_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    group_type VARCHAR(30) NOT NULL DEFAULT 'TEAM',
    branch_id UUID REFERENCES branches(id),
    department_id UUID REFERENCES departments(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Group Members
CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES org_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

-- Indexes
CREATE INDEX idx_branches_geo ON branches(geo_location_id);
CREATE INDEX idx_branches_parent ON branches(parent_branch_id);
CREATE INDEX idx_branches_status ON branches(status);
CREATE INDEX idx_departments_parent ON departments(parent_id);
CREATE INDEX idx_departments_branch ON departments(branch_id);
CREATE INDEX idx_groups_branch ON org_groups(branch_id);
CREATE INDEX idx_groups_dept ON org_groups(department_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
