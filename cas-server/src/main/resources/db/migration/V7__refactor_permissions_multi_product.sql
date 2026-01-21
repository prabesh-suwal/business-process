-- ============================================================================
-- V7: Refactor permissions to support multi-product assignment
-- ============================================================================

-- Add category column to permissions
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Create junction table for many-to-many relationship
CREATE TABLE IF NOT EXISTS product_permissions (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, permission_id)
);

-- Migrate existing permissions to junction table
INSERT INTO product_permissions (product_id, permission_id)
SELECT product_id, id FROM permissions
WHERE product_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_product_permissions_product ON product_permissions(product_id);
CREATE INDEX IF NOT EXISTS idx_product_permissions_permission ON product_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_permissions_code ON permissions(code);
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions(category);

-- Make product_id nullable (permissions can exist without a product initially)
ALTER TABLE permissions ALTER COLUMN product_id DROP NOT NULL;

-- Update unique constraint to just code (global unique)
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_product_id_code_key;
ALTER TABLE permissions ADD CONSTRAINT permissions_code_uk UNIQUE (code);
