-- =====================================================
-- V30: Rename 'variety' column to 'name' in sectors table
-- =====================================================
-- Description: Renames the 'variety' field to 'name' in metadata.sectors table.
--              The field remains NULLABLE as it was before.
-- Date: 2026-01-30
-- =====================================================

-- Rename column variety -> name
ALTER TABLE metadata.sectors RENAME COLUMN variety TO name;

-- Update column comment (optional, for documentation)
COMMENT ON COLUMN metadata.sectors.name IS 'Nombre o descripcion del sector (nullable)';
