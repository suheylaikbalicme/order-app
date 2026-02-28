CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(120) PRIMARY KEY,
    setting_value VARCHAR(2000),
    updated_at    TIMESTAMP
);

-- Defaults (idempotent)
INSERT INTO app_settings(setting_key, setting_value, updated_at)
VALUES
    ('ui.brandName', 'mr-CRM', NOW()),
    ('ui.footerText', '© mr-CRM', NOW()),
    ('sync.enabled', 'true', NOW()),
    ('fx.enabled', 'true', NOW()),
    ('logo.enabled', 'true', NOW())
ON CONFLICT (setting_key) DO NOTHING;
