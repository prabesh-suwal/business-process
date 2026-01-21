-- V1: Geographical Hierarchy Tables
-- Country-agnostic geographical structure (Nepal: Province/District/Municipality, USA: State/County/City)

-- Geo Hierarchy Types (defines levels per country)
CREATE TABLE geo_hierarchy_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(3) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    local_name VARCHAR(100),
    level INTEGER NOT NULL,
    parent_type_id UUID REFERENCES geo_hierarchy_types(id),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(country_code, code)
);

-- Geo Locations (actual places)
CREATE TABLE geo_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(3) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    local_name VARCHAR(150),
    type_id UUID NOT NULL REFERENCES geo_hierarchy_types(id),
    parent_id UUID REFERENCES geo_locations(id),
    full_path VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(country_code, code)
);

-- Indexes
CREATE INDEX idx_geo_locations_type ON geo_locations(type_id);
CREATE INDEX idx_geo_locations_parent ON geo_locations(parent_id);
CREATE INDEX idx_geo_locations_country ON geo_locations(country_code);
CREATE INDEX idx_geo_locations_status ON geo_locations(status);
