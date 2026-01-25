-- Make product_id nullable to support direct workflow deployments without ProcessTemplate
ALTER TABLE process_instance_metadata ALTER COLUMN product_id DROP NOT NULL;
