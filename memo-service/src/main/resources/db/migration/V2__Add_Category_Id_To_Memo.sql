ALTER TABLE memo ADD COLUMN category_id UUID REFERENCES memo_category(id);
