-- Add support for threaded replies and user mentions in comments

-- Self-referencing FK for threaded replies
ALTER TABLE memo_comment ADD COLUMN parent_comment_id UUID REFERENCES memo_comment(id);

-- Comma-separated list of mentioned user IDs
ALTER TABLE memo_comment ADD COLUMN mentioned_user_ids TEXT;

-- Index for efficient reply lookups
CREATE INDEX idx_memo_comment_parent ON memo_comment(parent_comment_id);
