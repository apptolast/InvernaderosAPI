-- =====================================================
-- V10: Extend alerts table for multi-tenant support
-- =====================================================
-- Description: Add multi-tenant fields and improve alerts table
-- Author: Claude Code
-- Date: 2025-11-16
-- =====================================================
-- Note: Table already exists from Kubernetes init script
-- This migration extends it with tenant_id and user FK
-- Paso 1: Agregar tenant_id (permite NULL temporalmente)
ALTER TABLE metadata.alerts
ADD COLUMN IF NOT EXISTS tenant_id UUID;
-- Paso 2: Agregar updated_at timestamp
ALTER TABLE metadata.alerts
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
-- Paso 3: Actualizar tenant_id desde greenhouses (denormalización)
UPDATE metadata.alerts a
SET tenant_id = g.tenant_id
FROM metadata.greenhouses g
WHERE a.greenhouse_id = g.id
    AND a.tenant_id IS NULL;
-- Paso 4: Establecer tenant_id como NOT NULL
ALTER TABLE metadata.alerts
ALTER COLUMN tenant_id
SET NOT NULL;
-- Paso 5: Crear columna temporal para migrar resolved_by de VARCHAR a UUID
ALTER TABLE metadata.alerts
ADD COLUMN IF NOT EXISTS resolved_by_user_id UUID;
-- Nota: Como resolved_by actualmente es VARCHAR(100), no podemos migrar automáticamente
-- Los valores existentes se mantendrán en resolved_by (VARCHAR) temporalmente
-- Nuevos registros deberán usar resolved_by_user_id (UUID)
-- Paso 6: Agregar Foreign Keys
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS fk_alerts_tenant;
ALTER TABLE metadata.alerts
ADD CONSTRAINT fk_alerts_tenant FOREIGN KEY (tenant_id) REFERENCES metadata.tenants(id) ON DELETE CASCADE;
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS fk_alerts_resolved_by_user;
ALTER TABLE metadata.alerts
ADD CONSTRAINT fk_alerts_resolved_by_user FOREIGN KEY (resolved_by_user_id) REFERENCES metadata.users(id) ON DELETE
SET NULL;
-- Ya existen FKs para greenhouse_id y sensor_id desde el init script
-- Paso 7: Actualizar CHECK constraint de severity para incluir nuevos niveles
-- El init script tiene: LOW, MEDIUM, HIGH, CRITICAL
-- Queremos: INFO, WARNING, ERROR, CRITICAL
-- Mantenemos ambos para compatibilidad
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS alerts_severity_check;
ALTER TABLE metadata.alerts
ADD CONSTRAINT chk_alert_severity CHECK (
        severity IN (
            'INFO',
            'WARNING',
            'ERROR',
            'CRITICAL',
            'LOW',
            'MEDIUM',
            'HIGH'
        )
    );
-- Paso 8: Agregar CHECK constraint para consistencia de resolved
ALTER TABLE metadata.alerts DROP CONSTRAINT IF EXISTS chk_resolved_consistency;
ALTER TABLE metadata.alerts
ADD CONSTRAINT chk_resolved_consistency CHECK (
        (
            is_resolved = FALSE
            AND resolved_at IS NULL
        )
        OR (
            is_resolved = TRUE
            AND resolved_at IS NOT NULL
        )
    );
-- Paso 9: Crear índices multi-tenant
CREATE INDEX IF NOT EXISTS idx_alerts_tenant ON metadata.alerts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alerts_tenant_unresolved ON metadata.alerts(tenant_id, is_resolved, created_at DESC)
WHERE is_resolved = FALSE;
CREATE INDEX IF NOT EXISTS idx_alerts_unresolved ON metadata.alerts(is_resolved, created_at DESC)
WHERE is_resolved = FALSE;
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON metadata.alerts(severity, created_at DESC)
WHERE is_resolved = FALSE;
CREATE INDEX IF NOT EXISTS idx_alerts_type ON metadata.alerts(alert_type, created_at DESC);
-- Reemplazar índice creado DESC existente
DROP INDEX IF EXISTS metadata.idx_alerts_created;
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON metadata.alerts(created_at DESC);
-- Paso 10: Crear índice GIN para JSONB alert_data
CREATE INDEX IF NOT EXISTS idx_alerts_data_gin ON metadata.alerts USING GIN (alert_data);
-- Paso 11: Actualizar comentarios
COMMENT ON TABLE metadata.alerts IS 'Sistema de alertas multi-tenant para monitoreo de invernaderos';
COMMENT ON COLUMN metadata.alerts.tenant_id IS 'ID del tenant (denormalizado para queries eficientes)';
COMMENT ON COLUMN metadata.alerts.alert_type IS 'Tipo: THRESHOLD_EXCEEDED, SENSOR_OFFLINE, ACTUATOR_FAILURE, SYSTEM_ERROR';
COMMENT ON COLUMN metadata.alerts.severity IS 'Severidad: INFO, WARNING, ERROR, CRITICAL (legacy: LOW, MEDIUM, HIGH)';
COMMENT ON COLUMN metadata.alerts.alert_data IS 'Contexto adicional JSONB: {"threshold": 30, "current_value": 35}';
COMMENT ON COLUMN metadata.alerts.resolved_by IS 'Usuario que resolvió (VARCHAR legacy, usar resolved_by_user_id)';
COMMENT ON COLUMN metadata.alerts.resolved_by_user_id IS 'UUID del usuario que resolvió (preferir sobre resolved_by)';
-- Paso 12: Mapear valores de severity antiguos a nuevos (opcional)
-- Descomentar si quieres migrar valores:
-- UPDATE metadata.alerts SET severity = 'INFO' WHERE severity = 'LOW';
-- UPDATE metadata.alerts SET severity = 'WARNING' WHERE severity = 'MEDIUM';
-- UPDATE metadata.alerts SET severity = 'ERROR' WHERE severity = 'HIGH';
-- CRITICAL se mantiene igual
-- Nota: Para futuras migraciones, considerar eliminar columna resolved_by (VARCHAR)
-- y usar solo resolved_by_user_id (UUID)