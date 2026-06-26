CREATE INDEX IF NOT EXISTS idx_message_content_fts
ON message
USING GIN (to_tsvector('simple', coalesce(content, '')));
