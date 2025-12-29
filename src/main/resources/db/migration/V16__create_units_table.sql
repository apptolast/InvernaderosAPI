-- ============================================================================
-- V16: Crear tabla de unidades de medida
-- Fecha: 2025-12-29
-- Descripcion: Tabla normalizada para unidades de medida (°C, %, hPa, etc.)
-- Referencia: https://www.postgresql.org/docs/current/datatype-character.html
-- ============================================================================

-- Crear tabla de unidades
CREATE TABLE IF NOT EXISTS metadata.units (
    id SMALLSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_units_symbol UNIQUE (symbol)
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_units_active ON metadata.units(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_units_symbol ON metadata.units(symbol);

-- Comentarios
COMMENT ON TABLE metadata.units IS 'Catalogo de unidades de medida para sensores y actuadores';
COMMENT ON COLUMN metadata.units.symbol IS 'Simbolo de la unidad (ej: °C, %, hPa)';
COMMENT ON COLUMN metadata.units.name IS 'Nombre descriptivo de la unidad';

-- Datos iniciales
INSERT INTO metadata.units (symbol, name, description) VALUES
    ('°C', 'Grados Celsius', 'Temperatura en grados Celsius'),
    ('°F', 'Grados Fahrenheit', 'Temperatura en grados Fahrenheit'),
    ('%', 'Porcentaje', 'Porcentaje (humedad, apertura, velocidad)'),
    ('hPa', 'Hectopascales', 'Presion atmosferica'),
    ('ppm', 'Partes por millon', 'Concentracion de CO2 u otros gases'),
    ('lux', 'Lux', 'Intensidad luminica'),
    ('W/m²', 'Vatios por metro cuadrado', 'Radiacion solar'),
    ('m/s', 'Metros por segundo', 'Velocidad del viento'),
    ('km/h', 'Kilometros por hora', 'Velocidad del viento'),
    ('mm', 'Milimetros', 'Precipitacion'),
    ('W', 'Vatios', 'Potencia electrica'),
    ('kW', 'Kilovatios', 'Potencia electrica'),
    ('L/h', 'Litros por hora', 'Caudal de riego'),
    ('L/min', 'Litros por minuto', 'Caudal de riego'),
    ('m³/h', 'Metros cubicos por hora', 'Caudal de aire'),
    ('RPM', 'Revoluciones por minuto', 'Velocidad de motor'),
    ('pH', 'pH', 'Acidez/alcalinidad'),
    ('mS/cm', 'Milisiemens por centimetro', 'Conductividad electrica'),
    ('dS/m', 'Decisiemens por metro', 'Conductividad electrica'),
    ('bar', 'Bar', 'Presion'),
    ('°', 'Grados', 'Angulo de apertura')
ON CONFLICT (symbol) DO NOTHING;
