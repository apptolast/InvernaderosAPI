-- ============================================================================
-- V19: Crear tabla de sectores
-- Fecha: 2025-12-29
-- Descripcion: Subdivisiones logicas de un invernadero
-- Referencia: https://www.postgresql.org/docs/current/datatype-json.html
-- ============================================================================

-- Crear tabla de sectores
CREATE TABLE IF NOT EXISTS metadata.sectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,

    -- Identificacion
    sector_code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Propiedades fisicas
    area_m2 NUMERIC(10,2),
    location_data JSONB,

    -- Tipo de sector
    sector_type VARCHAR(30),

    -- Cultivo especifico
    crop_type VARCHAR(100),
    crop_stage VARCHAR(30),

    -- Condiciones objetivo para este sector
    target_temperature_min NUMERIC(5,2),
    target_temperature_max NUMERIC(5,2),
    target_humidity_min NUMERIC(5,2),
    target_humidity_max NUMERIC(5,2),

    -- Estado
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_sector_code_per_greenhouse UNIQUE (greenhouse_id, sector_code),
    CONSTRAINT uq_sector_name_per_greenhouse UNIQUE (greenhouse_id, name),
    CONSTRAINT chk_sector_type CHECK (
        sector_type IS NULL OR sector_type IN ('PRODUCTION', 'NURSERY', 'STORAGE', 'IRRIGATION_ZONE', 'CLIMATE_ZONE', 'OTHER')
    ),
    CONSTRAINT chk_crop_stage CHECK (
        crop_stage IS NULL OR crop_stage IN ('SEEDLING', 'VEGETATIVE', 'FLOWERING', 'FRUITING', 'HARVEST', 'DORMANT')
    ),
    CONSTRAINT chk_temperature_range CHECK (
        target_temperature_min IS NULL OR target_temperature_max IS NULL OR
        target_temperature_min <= target_temperature_max
    ),
    CONSTRAINT chk_humidity_range CHECK (
        target_humidity_min IS NULL OR target_humidity_max IS NULL OR
        target_humidity_min <= target_humidity_max
    )
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_sectors_greenhouse ON metadata.sectors(greenhouse_id);
CREATE INDEX IF NOT EXISTS idx_sectors_active ON metadata.sectors(greenhouse_id, is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_sectors_code ON metadata.sectors(sector_code);
CREATE INDEX IF NOT EXISTS idx_sectors_type ON metadata.sectors(sector_type) WHERE sector_type IS NOT NULL;

-- Comentarios
COMMENT ON TABLE metadata.sectors IS 'Subdivisiones logicas de un invernadero para agrupar dispositivos';
COMMENT ON COLUMN metadata.sectors.sector_code IS 'Codigo corto unico dentro del greenhouse (S01, NORTE, ZONA_RIEGO_1)';
COMMENT ON COLUMN metadata.sectors.location_data IS 'Datos de ubicacion en formato JSON: {"position": "north", "row_start": 1, "row_end": 10}';
COMMENT ON COLUMN metadata.sectors.sector_type IS 'Tipo: PRODUCTION, NURSERY, STORAGE, IRRIGATION_ZONE, CLIMATE_ZONE, OTHER';
COMMENT ON COLUMN metadata.sectors.crop_stage IS 'Etapa del cultivo: SEEDLING, VEGETATIVE, FLOWERING, FRUITING, HARVEST, DORMANT';
