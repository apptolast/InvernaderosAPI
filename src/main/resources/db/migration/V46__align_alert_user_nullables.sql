-- M-2: Align metadata.alerts and metadata.users column nullability with the JPA entities.
-- The Kotlin entities declare these columns as @Column(nullable=false) but the database still
-- allowed NULL, so any straggler row would silently bypass entity validation and surface as a
-- runtime mismatch on read. Verified live (2026-05-04) that no rows currently hold NULL in any
-- of the six target columns; the UPDATE statements are kept as a defensive belt for any in-flight
-- writer that might land a NULL between the validation moment and the ALTER lock.

-- 1) Defensive backfill (no-ops today, blindaje frente a writes en vuelo)
UPDATE metadata.alerts SET is_resolved = (resolved_at IS NOT NULL) WHERE is_resolved IS NULL;
UPDATE metadata.alerts SET created_at  = now() WHERE created_at  IS NULL;
UPDATE metadata.alerts SET updated_at  = now() WHERE updated_at  IS NULL;
UPDATE metadata.users  SET is_active   = true  WHERE is_active   IS NULL;
UPDATE metadata.users  SET created_at  = now() WHERE created_at  IS NULL;
UPDATE metadata.users  SET updated_at  = now() WHERE updated_at  IS NULL;

-- 2) Enforce NOT NULL to match the entity declarations
ALTER TABLE metadata.alerts ALTER COLUMN is_resolved SET NOT NULL;
ALTER TABLE metadata.alerts ALTER COLUMN created_at  SET NOT NULL;
ALTER TABLE metadata.alerts ALTER COLUMN updated_at  SET NOT NULL;
ALTER TABLE metadata.users  ALTER COLUMN is_active   SET NOT NULL;
ALTER TABLE metadata.users  ALTER COLUMN created_at  SET NOT NULL;
ALTER TABLE metadata.users  ALTER COLUMN updated_at  SET NOT NULL;

-- 3) Defaults so future INSERTs without explicit values still land valid rows
ALTER TABLE metadata.alerts ALTER COLUMN is_resolved SET DEFAULT false;
ALTER TABLE metadata.alerts ALTER COLUMN created_at  SET DEFAULT now();
ALTER TABLE metadata.alerts ALTER COLUMN updated_at  SET DEFAULT now();
ALTER TABLE metadata.users  ALTER COLUMN is_active   SET DEFAULT true;
ALTER TABLE metadata.users  ALTER COLUMN created_at  SET DEFAULT now();
ALTER TABLE metadata.users  ALTER COLUMN updated_at  SET DEFAULT now();
