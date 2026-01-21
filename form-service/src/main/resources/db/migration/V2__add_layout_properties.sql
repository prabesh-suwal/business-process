-- V2: Add layout properties for enterprise form designer

-- Add new columns to field_definition table
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS element_type VARCHAR(20) DEFAULT 'field';
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS width VARCHAR(20) DEFAULT 'full';
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS custom_width INTEGER;
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS custom_height INTEGER;
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS label_position VARCHAR(20) DEFAULT 'top';
ALTER TABLE field_definition ADD COLUMN IF NOT EXISTS section_id VARCHAR(255);

-- Add comments for documentation
COMMENT ON COLUMN field_definition.element_type IS 'Type: field or layout';
COMMENT ON COLUMN field_definition.width IS 'Width preset: full, half, third, quarter';
COMMENT ON COLUMN field_definition.custom_width IS 'Custom width in pixels';
COMMENT ON COLUMN field_definition.custom_height IS 'Custom height in pixels';
COMMENT ON COLUMN field_definition.label_position IS 'Label position: top or left';
COMMENT ON COLUMN field_definition.section_id IS 'Reference to parent section field';
