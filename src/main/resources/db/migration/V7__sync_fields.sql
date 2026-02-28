-- Add sync tracking fields for "orders" and "customers".

ALTER TABLE orders
    ADD COLUMN sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN logo_ref VARCHAR(120),
    ADD COLUMN sync_error VARCHAR(2000),
    ADD COLUMN last_sync_at TIMESTAMPTZ;

ALTER TABLE customers
    ADD COLUMN last_sync_at TIMESTAMPTZ;
