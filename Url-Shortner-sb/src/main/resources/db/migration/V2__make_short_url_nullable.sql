-- short_url is now set in a follow-up UPDATE after the row's id is known (Base62-encoded id),
-- so it must briefly allow NULL between insert and that update. Postgres permits multiple NULLs
-- under a UNIQUE constraint, so this doesn't reopen the short-code uniqueness guarantee.
ALTER TABLE url_mapping ALTER COLUMN short_url DROP NOT NULL;
