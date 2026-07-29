-- Real-world URLs (tracking params, UTM tags, etc.) routinely exceed 255 chars,
-- which caused inserts to fail with "value too long for type character varying(255)".
ALTER TABLE url_mapping ALTER COLUMN original_url TYPE VARCHAR(2048);
