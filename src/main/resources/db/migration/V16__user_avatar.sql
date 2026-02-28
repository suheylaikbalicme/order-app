-- Add user avatar columns (stored in DB)
-- Safe to run on existing installations.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_data BYTEA;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(100);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_updated_at TIMESTAMPTZ;
