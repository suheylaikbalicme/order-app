-- Fix: some environments created audit_log.actor_username as BYTEA
-- Spring Data uses lower(actor_username) for *IgnoreCase* filters, which fails on BYTEA.
-- Convert to TEXT safely.

DO $$
BEGIN
  -- Only run if column exists and is BYTEA
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name='audit_log'
      AND column_name='actor_username'
      AND udt_name='bytea'
  ) THEN
    ALTER TABLE audit_log
      ALTER COLUMN actor_username TYPE TEXT
      USING (
        CASE
          WHEN actor_username IS NULL THEN NULL
          ELSE convert_from(actor_username, 'UTF8')
        END
      );

    -- Recreate index if needed (Postgres may keep it, but ensure it's valid)
    DROP INDEX IF EXISTS idx_audit_log_actor;
    CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log(actor_username);
  END IF;
END $$;
