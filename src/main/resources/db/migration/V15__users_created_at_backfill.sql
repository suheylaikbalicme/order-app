-- Ensure users.created_at exists even if the very first schema was created before this column
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'users'
      AND column_name = 'created_at'
  ) THEN
    ALTER TABLE users ADD COLUMN created_at timestamptz;
  END IF;

  -- Backfill nulls (older rows) and enforce defaults/constraints
  UPDATE users SET created_at = now() WHERE created_at IS NULL;

  ALTER TABLE users ALTER COLUMN created_at SET DEFAULT now();
  ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;
END $$;
