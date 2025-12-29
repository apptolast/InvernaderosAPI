-- ============================================================================
-- V17: Crear tabla de estados de actuadores
-- Fecha: 2025-12-29
-- Descripcion: Estados posibles para actuadores (ON, OFF, AUTO, ERROR, etc.)
-- Referencia: https://www.postgresql.org/docs/current/datatype-boolean.html
-- ============================================================================

-- Crear tabla de estados de actuadores
CREATE TABLE IF NOT EXISTS metadata.actuator_states (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    description TEXT,
    is_operational BOOLEAN NOT NULL DEFAULT FALSE,
    display_order SMALLINT NOT NULL DEFAULT 0,
    color VARCHAR(7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_actuator_states_name UNIQUE (name)
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_actuator_states_name ON metadata.actuator_states(name);
CREATE INDEX IF NOT EXISTS idx_actuator_states_operational ON metadata.actuator_states(is_operational);

-- Comentarios
COMMENT ON TABLE metadata.actuator_states IS 'Estados posibles para actuadores';
COMMENT ON COLUMN metadata.actuator_states.is_operational IS 'TRUE si el actuador esta funcionando en este estado';
COMMENT ON COLUMN metadata.actuator_states.display_order IS 'Orden para mostrar en UI';
COMMENT ON COLUMN metadata.actuator_states.color IS 'Color hexadecimal para UI (ej: #00FF00)';

-- Datos iniciales
INSERT INTO metadata.actuator_states (name, description, is_operational, display_order, color) VALUES
    ('OFF', 'Apagado', FALSE, 1, '#808080'),
    ('ON', 'Encendido', TRUE, 2, '#00FF00'),
    ('AUTO', 'Modo automatico', TRUE, 3, '#0066FF'),
    ('MANUAL', 'Control manual', TRUE, 4, '#FFA500'),
    ('ERROR', 'Error operacional', FALSE, 5, '#FF0000'),
    ('MAINTENANCE', 'En mantenimiento', FALSE, 6, '#FFFF00'),
    ('STANDBY', 'En espera', FALSE, 7, '#ADD8E6'),
    ('CALIBRATING', 'Calibrando', FALSE, 8, '#9932CC'),
    ('OFFLINE', 'Sin conexion', FALSE, 9, '#000000'),
    ('UNKNOWN', 'Estado desconocido', FALSE, 10, '#C0C0C0')
ON CONFLICT (name) DO NOTHING;
