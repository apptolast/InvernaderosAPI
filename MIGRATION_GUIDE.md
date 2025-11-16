# 🔄 GUÍA DE MIGRACIÓN MULTI-TENANT

## 📋 Resumen Ejecutivo

Esta guía describe el proceso completo de migración del sistema InvernaderosAPI de arquitectura single-tenant a multi-tenant, permitiendo gestionar múltiples empresas/clientes, cada una con N invernaderos.

**Duración estimada:** 2-3 horas (incluyendo backups y validaciones)
**Downtime necesario:** 30-60 minutos (solo durante aplicación de migraciones SQL críticas)
**Riesgo:** MEDIO (mitigado con backups completos y scripts de rollback)

---

## 🎯 Objetivos de la Migración

### Antes (Single-Tenant)
- ❌ Un único cliente/empresa por despliegue
- ❌ Topic MQTT fijo: `GREENHOUSE`
- ❌ `greenhouse_id` como VARCHAR en TimescaleDB
- ❌ Sin tenant_id en tablas

### Después (Multi-Tenant)
- ✅ Múltiples empresas en un solo despliegue
- ✅ Topics MQTT dinámicos: `GREENHOUSE/empresaID` (e.g., `GREENHOUSE/SARA`)
- ✅ `greenhouse_id` como UUID con integridad referencial
- ✅ `tenant_id` denormalizado para queries optimizados
- ✅ Compatibilidad con formato MQTT híbrido (JSON agregado + individual)

---

## ⚠️ PRE-REQUISITOS OBLIGATORIOS

### 1. Backups Completos

```bash
# Backup PostgreSQL (metadata - port 30433)
PGPASSWORD="AppToLast2023%" pg_dump -h 138.199.157.58 -p 30433 -U admin \
  -d postgres --schema=metadata --format=custom \
  -f backup_metadata_$(date +%Y%m%d_%H%M%S).dump

# Backup TimescaleDB (timeseries - port 30432)
PGPASSWORD="AppToLast2023%" pg_dump -h 138.199.157.58 -p 30432 -U admin \
  -d postgres --schema=public --table=sensor_readings --format=custom \
  -f backup_timescale_$(date +%Y%m%d_%H%M%S).dump
```

**CRÍTICO:** Verificar que los backups se crearon correctamente antes de continuar.

```bash
# Verificar tamaño de backups (deben ser > 0 bytes)
ls -lh backup_*.dump
```

### 2. Verificaciones Pre-Migración

```sql
-- Conectar a PostgreSQL metadata (port 30433)
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30433 -U admin -d postgres

-- VERIFICACIÓN 1: Contar tenants existentes (debe ser 0 antes de migración)
SELECT COUNT(*) FROM metadata.tenants;

-- VERIFICACIÓN 2: Contar greenhouses existentes
SELECT COUNT(*) FROM metadata.greenhouses;

-- VERIFICACIÓN 3: Contar sensores existentes
SELECT COUNT(*) FROM metadata.sensors;

-- VERIFICACIÓN 4: Verificar lecturas en TimescaleDB
\c postgres -h 138.199.157.58 -p 30432
SELECT COUNT(*) FROM public.sensor_readings;
SELECT COUNT(*) FROM public.sensor_readings WHERE greenhouse_id IS NULL;
-- IMPORTANTE: Si hay NULLs en greenhouse_id, la migración V8 FALLARÁ
```

### 3. Entorno de Staging/Pruebas

**RECOMENDACIÓN CRÍTICA:** Ejecutar esta migración PRIMERO en un entorno de staging idéntico a producción.

- ✅ Mismas versiones de PostgreSQL/TimescaleDB
- ✅ Misma estructura de datos
- ✅ Mismo volumen de datos (usar backup de producción)

---

## 📦 FASE 1: MIGRACIÓN BASE DE DATOS

### 1.1 Orden de Ejecución de Scripts SQL

**IMPORTANTE:** Los scripts Flyway se ejecutan automáticamente al arrancar la aplicación si están en `src/main/resources/db/migration/`. Sin embargo, para mayor control, se recomienda ejecutarlos manualmente en este orden:

```bash
# Conectar a PostgreSQL metadata (port 30433)
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30433 -U admin -d postgres

# Script 1: Expandir tabla tenants con campos de empresa
\i src/main/resources/db/migration/V3__add_tenant_company_fields.sql
-- Resultado esperado: 14 columnas añadidas, 2 constraints únicos

# Script 2: Añadir campos MQTT a greenhouses
\i src/main/resources/db/migration/V4__add_greenhouse_mqtt_fields.sql
-- Resultado esperado: 4 columnas añadidas, FK a tenants

# Script 3: Añadir campos MQTT a sensors + denormalizar tenant_id
\i src/main/resources/db/migration/V5__add_sensor_mqtt_fields.sql
-- Resultado esperado: 4 columnas añadidas, tenant_id propagado

# Script 4: Crear tabla actuators (NUEVA)
\i src/main/resources/db/migration/V6__create_actuators_table.sql
-- Resultado esperado: Tabla actuators creada con 17 campos

# Script 5: Migrar datos existentes a tenant DEFAULT
\i src/main/resources/db/migration/V7__migrate_existing_data.sql
-- Resultado esperado: Tenant DEFAULT creado, todos los datos migrados

# VERIFICACIÓN INTERMEDIA
SELECT * FROM metadata.tenants WHERE mqtt_topic_prefix = 'DEFAULT';
SELECT COUNT(*) FROM metadata.greenhouses WHERE tenant_id IS NOT NULL;
SELECT COUNT(*) FROM metadata.sensors WHERE tenant_id IS NOT NULL;
```

### 1.2 Migración Crítica TimescaleDB (UUID)

**⚠️ DOWNTIME REQUERIDO:** 15-30 minutos dependiendo del volumen de datos.

**¿Por qué es crítico?**
- Cambia `greenhouse_id` de VARCHAR(50) a UUID
- Puede tardar en tablas grandes (millones de filas)
- Requiere validación previa de datos

```bash
# Conectar a TimescaleDB (port 30432)
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30432 -U admin -d postgres

-- PRE-VALIDACIÓN (CRÍTICO)
-- Esta query DEBE devolver 0. Si devuelve > 0, hay datos inválidos.
SELECT COUNT(*) FROM public.sensor_readings WHERE greenhouse_id IS NULL;
SELECT COUNT(*) FROM public.sensor_readings WHERE greenhouse_id !~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

-- Si las validaciones pasan (COUNT = 0), ejecutar migración
\i src/main/resources/db/migration/V8__timescaledb_uuid_migration.sql

-- POST-VALIDACIÓN
SELECT
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'sensor_readings'
  AND column_name IN ('greenhouse_id', 'tenant_id');

-- Resultado esperado:
-- greenhouse_id | uuid | NO
-- tenant_id     | uuid | YES
```

### 1.3 Índices Multi-Tenant

```bash
# Script 6: Crear índices optimizados para queries multi-tenant
\i src/main/resources/db/migration/V9__add_indexes_multi_tenant.sql

-- VERIFICACIÓN: Contar índices creados
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE tablename IN ('tenants', 'greenhouses', 'sensors', 'actuators', 'sensor_readings')
ORDER BY tablename, indexname;
```

---

## 🔧 FASE 2: ACTUALIZACIÓN DE CÓDIGO

### 2.1 Dependencias de Repositorios

**VERIFICAR:** Los siguientes repositories deben existir con los métodos indicados.

```kotlin
// TenantRepository.kt (NUEVO)
interface TenantRepository : JpaRepository<Tenant, UUID> {
    fun findByMqttTopicPrefix(mqttTopicPrefix: String): Tenant?
    fun findByEmail(email: String): Tenant?
    fun findByTaxId(taxId: String): Tenant?
}

// GreenhouseRepository.kt (ACTUALIZADO)
interface GreenhouseRepository : JpaRepository<Greenhouse, UUID> {
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Greenhouse>
    fun findByMqttTopic(mqttTopic: String): Greenhouse?
}
```

### 2.2 Verificación de Entities

```bash
# Verificar que las entities tienen los campos UUID correctos
grep -n "greenhouseId: UUID" src/main/kotlin/com/apptolast/invernaderos/entities/timescaledb/entities/SensorReading.kt
grep -n "tenantId: UUID?" src/main/kotlin/com/apptolast/invernaderos/entities/timescaledb/entities/SensorReading.kt

# Resultado esperado:
# Línea XX: val greenhouseId: UUID,
# Línea YY: val tenantId: UUID? = null,
```

### 2.3 Configuración MQTT

**Archivo:** `src/main/resources/application.yaml`

Añadir/verificar la siguiente configuración:

```yaml
spring:
  mqtt:
    topics:
      greenhouse-multi-tenant: "GREENHOUSE/+"  # Nuevo topic pattern
```

---

## 🧪 FASE 3: PRUEBAS Y VALIDACIÓN

### 3.1 Test 1: Validación de Tenant DEFAULT

```sql
-- Conectar a PostgreSQL metadata
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30433 -U admin -d postgres

-- Verificar que existe tenant DEFAULT con datos migrados
SELECT
    t.id,
    t.name,
    t.mqtt_topic_prefix,
    COUNT(g.id) as num_greenhouses
FROM metadata.tenants t
LEFT JOIN metadata.greenhouses g ON g.tenant_id = t.id
WHERE t.mqtt_topic_prefix = 'DEFAULT'
GROUP BY t.id, t.name, t.mqtt_topic_prefix;

-- Resultado esperado: 1 tenant con N greenhouses (N = greenhouses existentes pre-migración)
```

### 3.2 Test 2: Crear Nuevo Tenant (SARA)

```sql
-- Crear tenant de prueba SARA
INSERT INTO metadata.tenants (
    id, name, email, company_name, tax_id,
    mqtt_topic_prefix, is_active, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'Sara Agro',
    'contacto@saraagro.com',
    'Sara Agrícola S.L.',
    'B12345678',
    'SARA',
    true,
    NOW(),
    NOW()
) RETURNING id, name, mqtt_topic_prefix;

-- Crear greenhouse para SARA
INSERT INTO metadata.greenhouses (
    id, tenant_id, name, greenhouse_code, mqtt_topic,
    mqtt_publish_interval_seconds, is_active, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    t.id,
    'Invernadero Sara 01',
    'SARA_01',
    'GREENHOUSE/SARA',
    5,
    true,
    NOW(),
    NOW()
FROM metadata.tenants t
WHERE t.mqtt_topic_prefix = 'SARA'
RETURNING id, name, mqtt_topic;
```

### 3.3 Test 3: Simular Mensaje MQTT Multi-Tenant

```bash
# Instalar mosquitto_pub si no está instalado
# sudo apt-get install mosquitto-clients

# Test con topic legacy (tenant DEFAULT)
mosquitto_pub -h <EMQX_BROKER> -p 1883 -u <USERNAME> -P <PASSWORD> \
  -t "GREENHOUSE" \
  -m '{"TEMPERATURA INVERNADERO 01":25.5,"HUMEDAD INVERNADERO 01":60.2}'

# Test con topic multi-tenant (SARA)
mosquitto_pub -h <EMQX_BROKER> -p 1883 -u <USERNAME> -P <PASSWORD> \
  -t "GREENHOUSE/SARA" \
  -m '{"TEMPERATURA INVERNADERO 01":23.1,"HUMEDAD INVERNADERO 01":58.7}'
```

### 3.4 Test 4: Verificar Procesamiento en Logs

```bash
# Logs de la aplicación Spring Boot
docker logs -f invernaderos-api --tail=100

# Buscar mensajes de validación tenant
grep "Tenant validado" logs.txt
grep "Greenhouse encontrado" logs.txt
grep "Procesamiento completado" logs.txt

# Resultado esperado:
# Tenant validado: Cliente Migración (UUID: xxx-xxx) [para GREENHOUSE]
# Tenant validado: Sara Agro (UUID: yyy-yyy) [para GREENHOUSE/SARA]
# Greenhouse encontrado: Invernadero Sara 01 (UUID: zzz-zzz)
# ✅ Procesamiento completado - Tenant: Sara Agro (SARA), Greenhouse: ... , 22 lecturas guardadas
```

### 3.5 Test 5: Verificar Datos en TimescaleDB

```sql
-- Conectar a TimescaleDB
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30432 -U admin -d postgres

-- Verificar lecturas con UUIDs correctos
SELECT
    time,
    sensor_id,
    greenhouse_id,
    tenant_id,
    sensor_type,
    value,
    unit
FROM public.sensor_readings
ORDER BY time DESC
LIMIT 10;

-- Verificar que tenant_id NO es NULL en lecturas nuevas
SELECT COUNT(*) FROM public.sensor_readings WHERE tenant_id IS NULL;
-- Resultado: Puede haber lecturas antiguas con NULL (antes de migración V8), pero las nuevas deben tener tenant_id

-- Verificar que greenhouse_id es UUID válido
SELECT
    greenhouse_id,
    COUNT(*) as num_readings
FROM public.sensor_readings
WHERE time > NOW() - INTERVAL '1 hour'
GROUP BY greenhouse_id;
```

---

## 📊 FASE 4: MONITOREO POST-MIGRACIÓN

### 4.1 Métricas Críticas

Monitorear durante las primeras 24-48 horas:

1. **Tasa de errores MQTT:**
   ```bash
   # Contar errores de validación tenant
   grep "Tenant no encontrado" logs.txt | wc -l
   # Debe ser 0 después de configuración correcta
   ```

2. **Latencia de procesamiento:**
   ```sql
   -- Diferencia entre timestamp del sensor y tiempo de guardado en DB
   SELECT
       AVG(EXTRACT(EPOCH FROM (time - time))) as avg_latency_seconds
   FROM public.sensor_readings
   WHERE time > NOW() - INTERVAL '1 hour';
   ```

3. **Integridad referencial:**
   ```sql
   -- Verificar que todos los greenhouse_id existen en metadata
   SELECT COUNT(*)
   FROM public.sensor_readings sr
   LEFT JOIN metadata.greenhouses g ON sr.greenhouse_id = g.id
   WHERE g.id IS NULL
     AND sr.time > NOW() - INTERVAL '1 hour';
   -- Debe devolver 0
   ```

### 4.2 Alertas Recomendadas

Configurar alertas para:
- ❌ `IllegalArgumentException: Tenant no encontrado` → Indica topic MQTT no configurado
- ❌ `IllegalStateException: No se encontró greenhouse activo` → Indica greenhouse inactivo o no creado
- ❌ Errors de conversión UUID → Indica datos corruptos en greenhouse_id

---

## 🔙 ROLLBACK (EN CASO DE EMERGENCIA)

### Opción 1: Rollback con Backups (RECOMENDADO)

```bash
# Restaurar PostgreSQL metadata
PGPASSWORD="AppToLast2023%" pg_restore -h 138.199.157.58 -p 30433 -U admin \
  -d postgres --clean --if-exists \
  backup_metadata_YYYYMMDD_HHMMSS.dump

# Restaurar TimescaleDB
PGPASSWORD="AppToLast2023%" pg_restore -h 138.199.157.58 -p 30432 -U admin \
  -d postgres --clean --if-exists \
  backup_timescale_YYYYMMDD_HHMMSS.dump

# Revertir código a versión anterior
git checkout <commit-hash-pre-migration>
./gradlew clean build -x test
docker-compose up -d --build
```

### Opción 2: Rollback Manual (Sin Backup)

**⚠️ PELIGROSO:** Solo usar si los backups no están disponibles.

```sql
-- Revertir V9 (índices)
DROP INDEX IF EXISTS metadata.idx_tenants_active;
DROP INDEX IF EXISTS metadata.idx_tenants_mqtt_prefix;
-- ... (continuar con todos los índices de V9)

-- Revertir V8 (UUID migration) - NO REVERSIBLE sin pérdida de datos
-- IMPOSIBLE revertir UUID → VARCHAR sin backup

-- Revertir V7 (datos migrados)
DELETE FROM metadata.tenants WHERE mqtt_topic_prefix = 'DEFAULT';

-- Revertir V6 (tabla actuators)
DROP TABLE IF EXISTS metadata.actuators CASCADE;

-- Revertir V5 (campos sensors)
ALTER TABLE metadata.sensors DROP COLUMN IF EXISTS sensor_code;
ALTER TABLE metadata.sensors DROP COLUMN IF EXISTS tenant_id;
-- ... (continuar con todas las columnas de V5)

-- Revertir V4 (campos greenhouses)
ALTER TABLE metadata.greenhouses DROP COLUMN IF EXISTS greenhouse_code;
-- ... (continuar con todas las columnas de V4)

-- Revertir V3 (campos tenants)
ALTER TABLE metadata.tenants DROP COLUMN IF EXISTS company_name;
-- ... (continuar con todas las columnas de V3)
```

---

## ✅ CHECKLIST FINAL PRE-PRODUCCIÓN

### Antes de Desplegar a Producción

- [ ] ✅ Migración ejecutada en staging sin errores
- [ ] ✅ Tests manuales pasados (5/5 tests de Fase 3)
- [ ] ✅ Backups completos realizados y verificados
- [ ] ✅ Equipo de soporte notificado de la ventana de mantenimiento
- [ ] ✅ Plan de rollback documentado y probado
- [ ] ✅ Monitoring/alertas configuradas
- [ ] ✅ Variables de entorno actualizadas (si aplica)
- [ ] ✅ Documentación de API actualizada (si hay cambios en endpoints)

### Durante el Despliegue

- [ ] ✅ Anunciar downtime a usuarios (si aplica)
- [ ] ✅ Detener aplicación (`docker-compose down`)
- [ ] ✅ Ejecutar backups inmediatos
- [ ] ✅ Ejecutar migraciones SQL (V3-V9 en orden)
- [ ] ✅ Validar migraciones con queries de verificación
- [ ] ✅ Desplegar código actualizado (`docker-compose up -d --build`)
- [ ] ✅ Monitorear logs en tiempo real (`docker logs -f invernaderos-api`)
- [ ] ✅ Ejecutar tests de humo (enviar mensajes MQTT de prueba)

### Post-Despliegue

- [ ] ✅ Verificar que sistema procesa mensajes MQTT correctamente
- [ ] ✅ Verificar que datos se guardan en TimescaleDB con UUIDs
- [ ] ✅ Verificar WebSocket broadcasting (si aplica)
- [ ] ✅ Monitorear métricas críticas durante 1 hora
- [ ] ✅ Confirmar 0 errores en logs
- [ ] ✅ Anunciar fin de mantenimiento

---

## 📚 REFERENCIA RÁPIDA

### Conexiones de Base de Datos

```bash
# PostgreSQL metadata (port 30433)
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30433 -U admin -d postgres

# TimescaleDB (port 30432)
PGPASSWORD="AppToLast2023%" psql -h 138.199.157.58 -p 30432 -U admin -d postgres
```

### Queries Útiles

```sql
-- Listar todos los tenants con su MQTT prefix
SELECT id, name, mqtt_topic_prefix, is_active FROM metadata.tenants;

-- Listar greenhouses por tenant
SELECT
    t.name as tenant_name,
    t.mqtt_topic_prefix,
    g.name as greenhouse_name,
    g.mqtt_topic,
    g.is_active
FROM metadata.tenants t
LEFT JOIN metadata.greenhouses g ON g.tenant_id = t.id
ORDER BY t.name, g.name;

-- Contar lecturas por tenant (últimas 24h)
SELECT
    t.name as tenant_name,
    COUNT(*) as num_readings
FROM public.sensor_readings sr
JOIN metadata.tenants t ON sr.tenant_id = t.id
WHERE sr.time > NOW() - INTERVAL '24 hours'
GROUP BY t.name
ORDER BY num_readings DESC;
```

### Logs y Debugging

```bash
# Ver logs en tiempo real
docker logs -f invernaderos-api --tail=100

# Filtrar errores críticos
docker logs invernaderos-api 2>&1 | grep "ERROR"

# Buscar mensajes de validación tenant
docker logs invernaderos-api 2>&1 | grep "Tenant validado"

# Ver uso de memoria/CPU del contenedor
docker stats invernaderos-api
```

---

## 🆘 CONTACTO Y SOPORTE

En caso de problemas durante la migración:

1. **STOP INMEDIATAMENTE** si se detectan errores críticos
2. Revisar logs completos: `docker logs invernaderos-api > migration_error.log`
3. Ejecutar queries de diagnóstico (sección Referencia Rápida)
4. Si es necesario, ejecutar ROLLBACK (sección anterior)
5. Contactar equipo de desarrollo con:
   - Logs completos (`migration_error.log`)
   - Queries de diagnóstico ejecutadas
   - Versión de código/commit hash
   - Hora exacta del error

---

## 📝 NOTAS FINALES

Esta migración ha sido diseñada para ser **segura, reversible y sin pérdida de datos**. Sin embargo, como cualquier operación de este tipo:

- ⚠️ **NUNCA** ejecutar en producción sin haber probado en staging
- ⚠️ **SIEMPRE** tener backups completos antes de empezar
- ⚠️ **VERIFICAR** cada paso antes de continuar con el siguiente
- ✅ **COMUNICAR** al equipo y usuarios sobre ventanas de mantenimiento

**Fecha de creación de esta guía:** 2025-11-16
**Versión:** 1.0
**Autor:** Claude Code (AI Assistant)
**Revisado por:** [PENDIENTE - completar después de revisión humana]

---

**¿Listo para comenzar?** 🚀
Sigue los pasos en orden y marca cada checkbox al completarlo. ¡Buena suerte!
