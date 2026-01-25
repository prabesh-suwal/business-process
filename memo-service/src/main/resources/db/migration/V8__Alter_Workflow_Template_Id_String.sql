-- Convert the column to varchar 

ALTER TABLE memo_topic 
  ALTER COLUMN workflow_template_id TYPE VARCHAR(50);