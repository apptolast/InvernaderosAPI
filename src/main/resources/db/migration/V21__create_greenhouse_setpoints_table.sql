-- ============================================================================
-- V21: Crear tabla de consignas/setpoints
-- Fecha: 2025-12-29
-- Descripcion: Consignas para parametros por periodo del dia (diurno/nocturno)
-- Referencia: https://www.postgresql.org/docs/current/datatype-datetime.html
-- ============================================================================

-- Verificar que existe la tabla alert_severities (puede no existir)
-- Si no existe, la creamos con valores basicos
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'metadata' AND table_name = 'alert_severities') THEN
        CREATE TABLE metadata.alert_severities (
            id SMALLSERIAL PRIMARY KEY,
            name VARCHAR(20) NOT NULL UNIQUE,
            level SMALLINT NOT NULL,
            description TEXT,
            color VARCHAR(7),
            requires_action BOOLEAN NOT NULL DEFAULT FALSE,
            notification_delay_minutes INTEGER DEFAULT 0,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );

        INSERT INTO metadata.alert_severities (name, level, description, color, requires_action) VALUES
            ('INFO', 1, 'Informativo', '#0066FF', FALSE),
            ('WARNING', 2, 'Advertencia', '#FFA500', FALSE),
            ('ERROR', 3, 'Error', '#FF6600', TRUE),
            ('CRITICAL', 4, 'Critico', '#FF0000', TRUE);
    END IF;
END $$;

-- Crear tabla de consignas
CREATE TABLE IF NOT EXISTS metadata.greenhouse_setpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    greenhouse_id UUID NOT NULL REFERENCES metadata.greenhouses(id) ON DELETE CASCADE,
    sector_id UUID REFERENCES metadata.sectors(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,

    -- Tipo de parametro
    parameter_type VARCHAR(30) NOT NULL,

    -- Periodo del dia
    day_period VARCHAR(20) NOT NULL,

    -- Valores de consigna
    min_value NUMERIC(10,2),
    max_value NUMERIC(10,2),
    target_value NUMERIC(10,2),

    -- Horarios del periodo
    start_time TIME,
    end_time TIME,

    -- Configuracion de alertas
    alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    alert_delay_minutes INTEGER NOT NULL DEFAULT 5,
    alert_severity_id SMALLINT REFERENCES metadata.alert_severities(id),

    -- Estado
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_setpoint_period CHECK (
        day_period IN ('DIURNO', 'NOCTURNO', 'ALL_DAY')
    ),
    CONSTRAINT chk_setpoint_parameter CHECK (
        parameter_type IN (
            'TEMPERATURE', 'HUMIDITY', 'CO2', 'LIGHT',
            'SOIL_MOISTURE', 'PRESSURE', 'WIND_SPEED', 'UV', 'PH', 'EC'
        )
    ),
    CONSTRAINT chk_setpoint_minmax CHECK (
        min_value IS NULL OR max_value IS NULL OR min_value <= max_value
    ),
    CONSTRAINT chk_setpoint_time CHECK (
        (day_period = 'ALL_DAY' AND start_time IS NULL AND end_time IS NULL) OR
        (day_period != 'ALL_DAY')
    ),
    CONSTRAINT chk_alert_delay CHECK (
        alert_delay_minutes >= 0 AND alert_delay_minutes <= 1440
    ),
    CONSTRAINT uq_setpoint_unique UNIQUE (greenhouse_id, sector_id, parameter_type, day_period)
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_setpoints_greenhouse ON metadata.greenhouse_setpoints(greenhouse_id);
CREATE INDEX IF NOT EXISTS idx_setpoints_sector ON metadata.greenhouse_setpoints(sector_id) WHERE sector_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_setpoints_tenant ON metadata.greenhouse_setpoints(tenant_id);
CREATE INDEX IF NOT EXISTS idx_setpoints_parameter ON metadata.greenhouse_setpoints(parameter_type);
CREATE INDEX IF NOT EXISTS idx_setpoints_period ON metadata.greenhouse_setpoints(day_period);
CREATE INDEX IF NOT EXISTS idx_setpoints_active ON metadata.greenhouse_setpoints(is_active) WHERE is_active = TRUE;

-- Indice compuesto para queries de verificacion de alertas
CREATE INDEX IF NOT EXISTS idx_setpoints_alert_check
    ON metadata.greenhouse_setpoints(greenhouse_id, parameter_type, is_active, alert_enabled)
    WHERE is_active = TRUE AND alert_enabled = TRUE;

-- Comentarios
COMMENT ON TABLE metadata.greenhouse_setpoints IS 'Consignas/setpoints para parametros por periodo del dia';
COMMENT ON COLUMN metadata.greenhouse_setpoints.day_period IS 'DIURNO, NOCTURNO, o ALL_DAY';
COMMENT ON COLUMN metadata.greenhouse_setpoints.start_time IS 'Hora de inicio del periodo (ej: 06:00 para diurno)';
COMMENT ON COLUMN metadata.greenhouse_setpoints.end_time IS 'Hora de fin del periodo (ej: 20:00 para diurno)';
COMMENT ON COLUMN metadata.greenhouse_setpoints.alert_delay_minutes IS 'Minutos de espera antes de generar alerta (evita falsos positivos)';
COMMENT ON COLUMN metadata.greenhouse_setpoints.sector_id IS 'NULL significa que aplica a todo el invernadero';

-- Vista para consulta rapida de setpoints activos
CREATE OR REPLACE VIEW metadata.v_active_setpoints AS
SELECT
    gs.id AS setpoint_id,
    gs.greenhouse_id,
    gs.sector_id,
    gs.tenant_id,
    gs.parameter_type,
    gs.day_period,
    gs.min_value,
    gs.max_value,
    gs.target_value,
    gs.start_time,
    gs.end_time,
    gs.alert_enabled,
    gs.alert_delay_minutes,
    asev.name AS alert_severity,
    asev.level AS severity_level,
    g.name AS greenhouse_name,
    sec.name AS sector_name,
    sec.sector_code
FROM metadata.greenhouse_setpoints gs
LEFT JOIN metadata.greenhouses g ON gs.greenhouse_id = g.id
LEFT JOIN metadata.sectors sec ON gs.sector_id = sec.id
LEFT JOIN metadata.alert_severities asev ON gs.alert_severity_id = asev.id
WHERE gs.is_active = TRUE;

COMMENT ON VIEW metadata.v_active_setpoints IS 'Vista de setpoints/consignas activas para facil consulta';
