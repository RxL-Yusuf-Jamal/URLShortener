-- Nullable: existing rows and links created without an explicit duration never expire.
ALTER TABLE url_mapping ADD COLUMN expires_at TIMESTAMP;