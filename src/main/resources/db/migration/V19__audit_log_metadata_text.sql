DO $$
BEGIN
    -- Some environments had `metadata` as JSONB. We store JSON as TEXT to avoid driver/hibernate binding issues.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'audit_log'
          AND column_name = 'metadata'
          AND data_type = 'jsonb'
    ) THEN
        ALTER TABLE audit_log
            ALTER COLUMN metadata TYPE TEXT
            USING metadata::text;
    END IF;
END $$;
