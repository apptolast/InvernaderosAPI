# Guía de Operaciones con Tablas Staging

## 📚 Tabla de Contenidos
- [Introducción](#introducción)
- [Arquitectura](#arquitectura)
- [Casos de Uso](#casos-de-uso)
- [Procedimientos Almacenados](#procedimientos-almacenados)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Mejores Prácticas](#mejores-prácticas)

---

## Introducción

Este sistema implementa **tablas staging e intermedias** para manejar operaciones masivas de datos (millones de registros) de forma segura, validada y con capacidad de rollback.

### ¿Por qué usar Staging?

✅ **Validación antes de insertar** - Detectar errores antes de contaminar producción
✅ **Rollback completo** - Revertir operaciones si algo sale mal
✅ **Auditoría** - Trazabilidad completa de todas las operaciones
✅ **Performance** - Batch processing optimizado para millones de registros
✅ **Testing seguro** - Dry-run mode para probar sin afectar datos

---

## Arquitectura

### TimescaleDB (Sensor Readings)

```
Datos MQTT/API
    ↓
staging.sensor_readings_raw (datos crudos, sin validar)
    ↓ [VALIDACIÓN]
staging.sensor_readings_validated (datos validados, listos)
    ↓ [MIGRACIÓN]
iot.sensor_readings (PRODUCCIÓN)
```

### PostgreSQL (Metadata)

```
Cambios Masivos
    ↓
staging.greenhouse_updates / staging.sensor_calibrations
    ↓ [APROBACIÓN]
metadata.greenhouses / metadata.sensors (PRODUCCIÓN)
```

---

## Casos de Uso

### 1. Importación Masiva de Datos MQTT (Millones de Registros)

**Problema:** Recibir 1M+ lecturas de sensores desde archivo CSV o bulk MQTT
**Solución:** Usar staging para validar antes de insertar

**Flujo:**
1. Insertar datos crudos en `staging.sensor_readings_raw`
2. Ejecutar validación con `staging.proc_validate_sensor_readings()`
3. Revisar errores de validación
4. Migrar datos válidos con `staging.proc_migrate_staging_to_production()`

### 2. Calibración Masiva de Sensores

**Problema:** Recalibrar 100+ sensores con nuevos rangos min/max
**Solución:** Staging con dry-run para verificar antes de aplicar

### 3. Corrección de Datos Históricos

**Problema:** Eliminar lecturas erróneas de un período específico
**Solución:** Data corrections staging con aprobación requerida

### 4. Actualización Masiva de Greenhouses

**Problema:** Cambiar configuración MQTT de múltiples invernaderos
**Solución:** Greenhouse updates staging con validación

---

## Procedimientos Almacenados

### TimescaleDB

#### `staging.proc_validate_sensor_readings(p_batch_id UUID)`

Valida datos crudos aplicando reglas configurables.

**Validaciones aplicadas:**
- Campos obligatorios (time, sensor_id, greenhouse_id, value)
- Formato UUID correcto
- Rangos de valores (según `staging.validation_rules`)

**Retorna:**
```sql
total_processed | total_valid | total_invalid | validation_summary
----------------+-------------+---------------+-------------------
        10000   |     9850    |      150      | {"batch_id": "..."}
```

#### `staging.proc_migrate_staging_to_production(p_batch_id UUID, p_delete_staging_after BOOLEAN)`

Migra datos validados a producción.

**Parámetros:**
- `p_batch_id`: UUID del batch a migrar
- `p_delete_staging_after`: TRUE para limpiar staging después (default: TRUE)

**Retorna:**
```sql
inserted_count | duration_seconds | status
---------------+------------------+-----------
      9850     |       45         | COMPLETED
```

#### `staging.proc_cleanup_staging(p_days_to_keep INT)`

Limpia datos antiguos de staging.

**Parámetros:**
- `p_days_to_keep`: Días de retención (default: 7)

### PostgreSQL

#### `staging.proc_apply_greenhouse_updates(p_batch_id UUID, p_dry_run BOOLEAN)`

Aplica actualizaciones masivas de greenhouses.

**Parámetros:**
- `p_batch_id`: UUID del batch
- `p_dry_run`: TRUE para simular sin aplicar cambios (default: FALSE)

**Retorna:**
```sql
operations_applied | inserts_count | updates_count | deletes_count | status
-------------------+---------------+---------------+---------------+------------
        250        |      100      |      140      |      10       | COMPLETED
```

#### `staging.proc_apply_sensor_calibrations(p_batch_id UUID)`

Aplica calibraciones masivas de sensores.

#### `staging.proc_rollback_operation(p_operation_id UUID)`

Revierte una operación usando rollback SQL almacenado.

---

## Ejemplos de Uso

### Ejemplo 1: Importación Masiva desde CSV

```sql
-- PASO 1: Insertar datos crudos en staging
INSERT INTO staging.sensor_readings_raw (
    time, sensor_id, greenhouse_id, tenant_id, sensor_type, value, unit, batch_id, source
)
SELECT
    timestamp_column::TIMESTAMPTZ,
    sensor_id_column,
    greenhouse_id_column,
    tenant_id_column,
    sensor_type_column,
    value_column::DOUBLE PRECISION,
    unit_column,
    gen_random_uuid(),  -- Generar batch_id único
    'CSV_IMPORT'
FROM staging_csv_import_temp;

-- PASO 2: Validar datos
SELECT * FROM staging.proc_validate_sensor_readings(NULL);
-- Retorna: total_processed=1000000, total_valid=998500, total_invalid=1500

-- PASO 3: Revisar errores de validación
SELECT sensor_id, validation_errors, COUNT(*)
FROM staging.sensor_readings_raw
WHERE validation_status = 'INVALID'
GROUP BY sensor_id, validation_errors;

-- PASO 4: Migrar datos válidos a producción
SELECT * FROM staging.proc_migrate_staging_to_production(
    'batch-uuid-aqui',
    TRUE  -- Limpiar staging después
);
-- Retorna: inserted_count=998500, duration_seconds=120, status='COMPLETED'
```

### Ejemplo 2: Actualización Masiva de Greenhouses

```sql
-- PASO 1: Preparar actualizaciones en staging
INSERT INTO staging.greenhouse_updates (
    operation, greenhouse_id, mqtt_publish_interval_seconds, batch_id, submitted_by
)
SELECT
    'UPDATE',
    id,
    10,  -- Cambiar intervalo a 10 segundos
    gen_random_uuid(),
    'admin_user'
FROM metadata.greenhouses
WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440001';

-- PASO 2: Simular aplicación (DRY RUN)
SELECT * FROM staging.proc_apply_greenhouse_updates(
    'batch-uuid-aqui',
    TRUE  -- DRY RUN
);
-- Revisa resultados sin aplicar cambios

-- PASO 3: Aplicar cambios reales
SELECT * FROM staging.proc_apply_greenhouse_updates(
    'batch-uuid-aqui',
    FALSE  -- Aplicar realmente
);
```

### Ejemplo 3: Calibración Masiva de Sensores

```sql
-- PASO 1: Registrar calibraciones
INSERT INTO staging.sensor_calibrations (
    sensor_id, calibration_type,
    old_min_threshold, old_max_threshold,
    new_min_threshold, new_max_threshold,
    batch_id, reason, submitted_by
)
SELECT
    id,
    'RANGE_ADJUSTMENT',
    min_threshold,
    max_threshold,
    min_threshold - 5,  -- Ampliar rango inferior
    max_threshold + 5,  -- Ampliar rango superior
    gen_random_uuid(),
    'Recalibración post-mantenimiento 2025-11',
    'tech_team'
FROM metadata.sensors
WHERE sensor_type = 'TEMPERATURE'
  AND tenant_id = '550e8400-e29b-41d4-a716-446655440002';

-- PASO 2: Aplicar calibraciones
SELECT * FROM staging.proc_apply_sensor_calibrations('batch-uuid-aqui');
```

### Ejemplo 4: Monitoreo de Operaciones

```sql
-- Ver resumen de todas las operaciones en staging
SELECT * FROM staging.v_operations_summary;

-- Ver log de operaciones masivas recientes
SELECT
    batch_id,
    operation_type,
    total_records,
    successful_records,
    failed_records,
    duration_seconds,
    status,
    started_at
FROM staging.bulk_import_log
ORDER BY started_at DESC
LIMIT 10;

-- Ver reglas de validación activas
SELECT sensor_type, rule_name, rule_config
FROM staging.validation_rules
WHERE is_active = TRUE;
```

### Ejemplo 5: Rollback de Operación

```sql
-- Buscar operación reciente
SELECT operation_id, operation_type, executed_at, rows_affected, status
FROM staging.operation_audit_log
WHERE executed_at > NOW() - INTERVAL '1 hour'
ORDER BY executed_at DESC;

-- Revertir operación si es necesario
SELECT * FROM staging.proc_rollback_operation('operation-uuid-aqui');
```

---

## Mejores Prácticas

### 1. Usa Batch IDs Únicos

```sql
-- ✅ CORRECTO: Generar UUID único por batch
SELECT gen_random_uuid();

-- ❌ INCORRECTO: Reutilizar batch_id
INSERT INTO staging.sensor_readings_raw (..., batch_id)
VALUES (..., 'same-uuid-for-everything');  -- NO HACER ESTO
```

### 2. Siempre Valida Antes de Migrar

```sql
-- ✅ CORRECTO: Workflow completo
INSERT INTO staging.sensor_readings_raw (...);  -- 1. Insertar
SELECT * FROM staging.proc_validate_sensor_readings(...);  -- 2. Validar
-- 3. Revisar errores antes de continuar
SELECT * FROM staging.proc_migrate_staging_to_production(...);  -- 4. Migrar

-- ❌ INCORRECTO: Saltar validación
INSERT INTO iot.sensor_readings (...);  -- Insertar directo a producción
```

### 3. Usa DRY RUN para Operaciones Críticas

```sql
-- ✅ CORRECTO: Probar primero
SELECT * FROM staging.proc_apply_greenhouse_updates(batch_id, TRUE);  -- DRY RUN
-- Revisar resultados, luego:
SELECT * FROM staging.proc_apply_greenhouse_updates(batch_id, FALSE);  -- REAL

-- ❌ INCORRECTO: Aplicar sin probar
SELECT * FROM staging.proc_apply_greenhouse_updates(batch_id, FALSE);
```

### 4. Limpia Staging Regularmente

```sql
-- ✅ CORRECTO: Limpieza automática semanal
SELECT * FROM staging.proc_cleanup_staging(7);  -- Retener 7 días

-- ❌ INCORRECTO: Nunca limpiar
-- Staging crece indefinidamente, afectando performance
```

### 5. Monitorea el Log de Auditoría

```sql
-- ✅ CORRECTO: Revisar regularmente
SELECT * FROM staging.operation_audit_log
WHERE status IN ('FAILED', 'RUNNING')
  AND executed_at > NOW() - INTERVAL '1 day';

-- Crear alertas para operaciones fallidas
```

### 6. Configura Reglas de Validación por Sensor

```sql
-- ✅ CORRECTO: Rangos específicos por tipo
INSERT INTO staging.validation_rules (sensor_type, rule_name, rule_type, rule_config)
VALUES (
    'CUSTOM_SENSOR',
    'valid_range',
    'RANGE',
    '{"min": 0, "max": 500, "unit": "custom"}'::JSONB
);

-- Actualizar reglas existentes
UPDATE staging.validation_rules
SET rule_config = '{"min": -10, "max": 50}'::JSONB
WHERE sensor_type = 'TEMPERATURE' AND rule_name = 'valid_range';
```

---

## Continuous Aggregates (TimescaleDB)

### Datos Pre-Agregados Disponibles

#### `iot.sensor_readings_hourly`

Agregación automática por hora con estadísticas.

```sql
-- Consultar temperatura promedio por hora, últimas 24 horas
SELECT
    hour,
    sensor_id,
    avg_value,
    min_value,
    max_value
FROM iot.sensor_readings_hourly
WHERE sensor_type = 'TEMPERATURE'
  AND greenhouse_id = '660e8400-e29b-41d4-a716-446655440001'
  AND hour > NOW() - INTERVAL '24 hours'
ORDER BY hour DESC;
```

**Campos disponibles:**
- `hour` - Timestamp del bucket horario
- `sensor_id`, `greenhouse_id`, `tenant_id`, `sensor_type`
- `reading_count` - Número de lecturas
- `avg_value`, `min_value`, `max_value` - Estadísticas
- `stddev_value` - Desviación estándar
- `median_value` - Mediana

#### `iot.sensor_readings_daily_by_tenant`

Agregación diaria multi-tenant.

```sql
-- Comparar actividad entre tenants
SELECT
    day,
    t.name as tenant_name,
    r.sensor_type,
    r.total_readings,
    r.unique_greenhouses,
    r.avg_value
FROM iot.sensor_readings_daily_by_tenant r
JOIN metadata.tenants t ON t.id = r.tenant_id
WHERE r.day > NOW() - INTERVAL '7 days'
ORDER BY r.day DESC, r.total_readings DESC;
```

---

## Troubleshooting

### Problema: Validación muy lenta con millones de registros

**Solución:** Procesar en batches más pequeños

```sql
-- Validar en batches de 100K registros
DO $$
DECLARE
    v_batch_size INT := 100000;
    v_offset INT := 0;
    v_total_processed INT := 0;
BEGIN
    LOOP
        -- Marcar batch temporal
        UPDATE staging.sensor_readings_raw
        SET batch_id = gen_random_uuid()
        WHERE id IN (
            SELECT id FROM staging.sensor_readings_raw
            WHERE validation_status = 'PENDING'
            LIMIT v_batch_size
        );

        -- Validar batch
        PERFORM staging.proc_validate_sensor_readings(NULL);

        v_total_processed := v_total_processed + v_batch_size;

        EXIT WHEN NOT EXISTS (
            SELECT 1 FROM staging.sensor_readings_raw
            WHERE validation_status = 'PENDING'
        );
    END LOOP;

    RAISE NOTICE 'Validación completada: % registros procesados', v_total_processed;
END $$;
```

### Problema: Migración a producción falla por duplicados

**Solución:** Ya manejado con `ON CONFLICT DO NOTHING`

```sql
-- La migración ignora duplicados automáticamente
-- Ver en V11__create_staging_infrastructure_timescaledb.sql línea ~580
INSERT INTO iot.sensor_readings (...)
SELECT ...
FROM staging.sensor_readings_validated
ON CONFLICT (time, sensor_id) DO NOTHING;  -- ← Ignora duplicados
```

### Problema: No puedo revertir una operación

**Solución:** Asegurar que la operación se registró con rollback_sql

```sql
-- Verificar si hay rollback disponible
SELECT rollback_available, rollback_sql
FROM staging.operation_audit_log
WHERE operation_id = 'operation-uuid';

-- Si rollback_available = FALSE, no se puede revertir automáticamente
-- Necesitarás crear un rollback manual
```

---

## Performance Tips

### 1. Índices en Staging

Los índices ya están creados en las tablas staging, pero si ves lentitud:

```sql
-- Ver índices existentes
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'staging';

-- Crear índice adicional si necesitas filtrar por otros campos
CREATE INDEX idx_custom ON staging.sensor_readings_raw(field_name)
WHERE condition;
```

### 2. Vacuum Regular

```sql
-- Después de grandes operaciones de staging
VACUUM ANALYZE staging.sensor_readings_raw;
VACUUM ANALYZE staging.sensor_readings_validated;
```

### 3. Partition Staging Tables (Opcional)

Para workloads extremadamente grandes (>100M registros/día), considera particionar:

```sql
-- Ejemplo: Particionar por fecha de recepción
CREATE TABLE staging.sensor_readings_raw_2025_11 PARTITION OF staging.sensor_readings_raw
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
```

---

## Resumen de Comandos Frecuentes

```sql
-- IMPORTACIÓN MASIVA COMPLETA
-- 1. Insertar datos crudos
INSERT INTO staging.sensor_readings_raw (...) VALUES (...);

-- 2. Validar
SELECT * FROM staging.proc_validate_sensor_readings(NULL);

-- 3. Revisar errores
SELECT * FROM staging.sensor_readings_raw WHERE validation_status = 'INVALID';

-- 4. Migrar a producción
SELECT * FROM staging.proc_migrate_staging_to_production('batch-uuid', TRUE);

-- LIMPIEZA SEMANAL
SELECT * FROM staging.proc_cleanup_staging(7);

-- MONITOREO
SELECT * FROM staging.v_operations_summary;
SELECT * FROM staging.bulk_import_log ORDER BY started_at DESC LIMIT 10;

-- ROLLBACK
SELECT * FROM staging.proc_rollback_operation('operation-uuid');
```

---

## Contacto y Soporte

Para dudas o problemas con el sistema de staging:
- Revisar logs: `staging.bulk_import_log`, `staging.operation_audit_log`
- Documentación adicional: Ver comentarios en los scripts SQL (V11)
- Código fuente: `V11__create_staging_infrastructure_*.sql`

---

**Autor:** Claude Code
**Versión:** 1.0
**Fecha:** 2025-11-16
**Base de Datos:** PostgreSQL 16 + TimescaleDB
