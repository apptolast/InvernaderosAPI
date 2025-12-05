# 🚨 ESTRATEGIA CRÍTICA DE DATOS - InvernaderosAPI

**Fecha:** 2025-12-05
**Autor:** Análisis profundo del sistema
**Status:** ACCIÓN INMEDIATA REQUERIDA

---

## 📊 DIAGNÓSTICO DEL PROBLEMA

### Problema Principal Identificado

**¡EL SISTEMA ESTÁ GUARDANDO ~5x MÁS DATOS DE LO NECESARIO!**

```
┌─────────────┬─────────────────┬───────────────────┬──────────────────────┐
│ Día         │ Lecturas        │ Mensajes/minuto   │ Segundos entre msgs  │
├─────────────┼─────────────────┼───────────────────┼──────────────────────┤
│ Nov 29-Dec1 │ ~864,000        │ ~24               │ ~2.5 seg             │
│ Dec 3-4     │ ~4,300,000      │ ~130              │ ~0.4 seg ⚠️ 5x MÁS │
│ Dec 5       │ ~2,400,000*     │ ~75               │ ~0.8 seg ⚠️ 3x MÁS │
└─────────────┴─────────────────┴───────────────────┴──────────────────────┘
* Parcial (día en curso)
```

### Causas Raíz Identificadas

#### 1. ⚠️ MÚLTIPLES RÉPLICAS CON SIMULACIÓN ACTIVADA

```bash
# PRODUCCIÓN tiene 2 réplicas ejecutando la simulación SIMULTÁNEAMENTE
kubectl get pods -n apptolast-invernadero-api-prod
  invernaderos-api-c5b6df5d8-hhj25   1/1  Running  # SIMULACIÓN ACTIVA
  invernaderos-api-c5b6df5d8-xw4fm   1/1  Running  # SIMULACIÓN ACTIVA ← DUPLICADO

# DESARROLLO también tiene la simulación activa
kubectl get pods -n apptolast-invernadero-api-dev
  invernaderos-api-7757d88788-4qzzv  1/1  Running  # SIMULACIÓN ACTIVA ← TRIPLICADO
```

**Resultado:** 3 instancias generando datos cada 5 segundos = ~2.5 mensajes/segundo en lugar de 1 cada 5 segundos.

#### 2. ⚠️ ARQUITECTURA DE ALMACENAMIENTO INEFICIENTE

Cada mensaje MQTT contiene 22 campos de sensores. Actualmente:
- **1 mensaje MQTT = 25 filas en la base de datos** (una fila por sensor + 3 metadatos)
- **~114,655 mensajes/día** con 22 sensores = **~2.5 millones de filas/día**

#### 3. 📐 CÁLCULO DEL PROBLEMA

```
Frecuencia actual:      ~75 msgs/minuto (debería ser ~12 msgs/minuto)
Lecturas por mensaje:   22 sensores + 3 metadatos = 25 filas
Filas por día actual:   75 × 60 × 24 × 22 = ~2.4M filas/día
Filas por día normal:   12 × 60 × 24 × 22 = ~380K filas/día

EXCESO: ~6x más datos de lo necesario
```

---

## 🎯 ESTRATEGIA DE SOLUCIÓN MULTINIVEL

### NIVEL 1: SOLUCIÓN INMEDIATA (HOY)

#### A. Desactivar Simulación en Réplicas Extra

```bash
# OPCIÓN 1: Reducir réplicas en producción a 1 (recomendado si solo hay simulación)
kubectl scale deployment/invernaderos-api -n apptolast-invernadero-api-prod --replicas=1

# OPCIÓN 2: Desactivar simulación en producción (mejor opción si hay datos reales)
kubectl set env deployment/invernaderos-api -n apptolast-invernadero-api-prod \
  GREENHOUSE_SIMULATION_ENABLED=false

# Desactivar simulación en desarrollo también
kubectl set env deployment/invernaderos-api -n apptolast-invernadero-api-dev \
  GREENHOUSE_SIMULATION_ENABLED=false
```

#### B. Aumentar Intervalo de Simulación

Si necesitas mantener la simulación, aumentar el intervalo:

```yaml
# En configmap o deployment
greenhouse:
  simulation:
    enabled: true
    interval-ms: 60000  # 1 minuto en lugar de 5 segundos
```

---

### NIVEL 2: OPTIMIZACIÓN DE ARQUITECTURA (ESTA SEMANA)

#### A. Cambiar Modelo de Datos: De Filas a Columnas

**ACTUAL (ineficiente):**
```sql
-- 22 filas por cada mensaje (una por sensor)
| time                | sensor_id                   | value |
|---------------------|-----------------------------| ------|
| 2025-12-05 12:00:00 | TEMPERATURA INVERNADERO 01  | 25.3  |
| 2025-12-05 12:00:00 | HUMEDAD INVERNADERO 01      | 60.2  |
| 2025-12-05 12:00:00 | TEMPERATURA INVERNADERO 02  | 24.1  |
... (22 filas por timestamp)
```

**PROPUESTO (eficiente):**
```sql
-- 1 fila por mensaje con todos los sensores
| time                | temp_01 | hum_01 | temp_02 | hum_02 | ... |
|---------------------|---------|--------|---------|--------|-----|
| 2025-12-05 12:00:00 | 25.3    | 60.2   | 24.1    | 58.5   | ... |
```

**Reducción:** De 25 filas a 1 fila por mensaje = **96% menos filas**

#### B. Implementar Tabla Denormalizada

```sql
-- Nueva tabla optimizada para datos de invernadero
CREATE TABLE iot.greenhouse_readings (
    time TIMESTAMPTZ NOT NULL,
    greenhouse_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    
    -- Temperaturas
    temp_invernadero_01 DOUBLE PRECISION,
    temp_invernadero_02 DOUBLE PRECISION,
    temp_invernadero_03 DOUBLE PRECISION,
    
    -- Humedades
    hum_invernadero_01 DOUBLE PRECISION,
    hum_invernadero_02 DOUBLE PRECISION,
    hum_invernadero_03 DOUBLE PRECISION,
    
    -- Sectores (12 campos)
    sector_01_01 DOUBLE PRECISION,
    sector_01_02 DOUBLE PRECISION,
    sector_01_03 DOUBLE PRECISION,
    sector_01_04 DOUBLE PRECISION,
    sector_02_01 DOUBLE PRECISION,
    sector_02_02 DOUBLE PRECISION,
    sector_02_03 DOUBLE PRECISION,
    sector_02_04 DOUBLE PRECISION,
    sector_03_01 DOUBLE PRECISION,
    sector_03_02 DOUBLE PRECISION,
    sector_03_03 DOUBLE PRECISION,
    sector_03_04 DOUBLE PRECISION,
    
    -- Extractores
    extractor_01 DOUBLE PRECISION,
    extractor_02 DOUBLE PRECISION,
    extractor_03 DOUBLE PRECISION,
    
    -- Reserva
    reserva DOUBLE PRECISION,
    
    PRIMARY KEY (time, greenhouse_id)
);

-- Convertir a hypertable
SELECT create_hypertable('iot.greenhouse_readings', 'time', 
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Configurar compresión agresiva
ALTER TABLE iot.greenhouse_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'greenhouse_id, tenant_id',
    timescaledb.compress_orderby = 'time DESC'
);

-- Comprimir después de 6 horas (más agresivo)
SELECT add_compression_policy('iot.greenhouse_readings', 
    compress_after => INTERVAL '6 hours',
    if_not_exists => TRUE
);
```

---

### NIVEL 3: DOWNSAMPLING JERÁRQUICO (PRÓXIMA SEMANA)

#### Estrategia de Agregación Multi-Nivel

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PIRÁMIDE DE DATOS                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│    NIVEL 4: Mensuales (retención: INDEFINIDA)                       │
│    └── 1 fila por invernadero por mes                               │
│        └── Agregados: avg, min, max, stddev                         │
│                                                                     │
│    NIVEL 3: Diarios (retención: 2 AÑOS)                             │
│    └── 1 fila por invernadero por día                               │
│        └── Agregados: avg, min, max, stddev, percentiles            │
│                                                                     │
│    NIVEL 2: Horarios (retención: 90 DÍAS)                           │
│    └── 1 fila por invernadero por hora                              │
│        └── Agregados: avg, min, max, count                          │
│                                                                     │
│    NIVEL 1: Datos crudos (retención: 7 DÍAS)                        │
│    └── 1 fila por mensaje (~1/minuto)                               │
│        └── Datos completos sin agregar                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Implementación SQL

```sql
-- ═══════════════════════════════════════════════════════════════════
-- CONTINUOUS AGGREGATE NIVEL 2: HORARIO
-- ═══════════════════════════════════════════════════════════════════
CREATE MATERIALIZED VIEW iot.greenhouse_hourly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS bucket,
    greenhouse_id,
    tenant_id,
    
    -- Temperatura promedio, min, max
    AVG(temp_invernadero_01) AS avg_temp_01,
    MIN(temp_invernadero_01) AS min_temp_01,
    MAX(temp_invernadero_01) AS max_temp_01,
    AVG(temp_invernadero_02) AS avg_temp_02,
    MIN(temp_invernadero_02) AS min_temp_02,
    MAX(temp_invernadero_02) AS max_temp_02,
    AVG(temp_invernadero_03) AS avg_temp_03,
    MIN(temp_invernadero_03) AS min_temp_03,
    MAX(temp_invernadero_03) AS max_temp_03,
    
    -- Humedad promedio
    AVG(hum_invernadero_01) AS avg_hum_01,
    MIN(hum_invernadero_01) AS min_hum_01,
    MAX(hum_invernadero_01) AS max_hum_01,
    AVG(hum_invernadero_02) AS avg_hum_02,
    AVG(hum_invernadero_03) AS avg_hum_03,
    
    -- Conteo de lecturas
    COUNT(*) AS reading_count
FROM iot.greenhouse_readings
GROUP BY bucket, greenhouse_id, tenant_id
WITH NO DATA;

-- Política de refresh (cada 15 minutos, datos de última semana)
SELECT add_continuous_aggregate_policy('iot.greenhouse_hourly',
    start_offset => INTERVAL '7 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '15 minutes',
    if_not_exists => TRUE
);

-- ═══════════════════════════════════════════════════════════════════
-- CONTINUOUS AGGREGATE NIVEL 3: DIARIO (sobre horario)
-- ═══════════════════════════════════════════════════════════════════
CREATE MATERIALIZED VIEW iot.greenhouse_daily
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 day', bucket) AS bucket,
    greenhouse_id,
    tenant_id,
    
    -- Agregados del día
    AVG(avg_temp_01) AS avg_temp_01,
    MIN(min_temp_01) AS min_temp_01,
    MAX(max_temp_01) AS max_temp_01,
    AVG(avg_temp_02) AS avg_temp_02,
    AVG(avg_temp_03) AS avg_temp_03,
    
    AVG(avg_hum_01) AS avg_hum_01,
    MIN(min_hum_01) AS min_hum_01,
    MAX(max_hum_01) AS max_hum_01,
    
    SUM(reading_count) AS total_readings
FROM iot.greenhouse_hourly
GROUP BY bucket, greenhouse_id, tenant_id
WITH NO DATA;

-- Refresh cada 6 horas
SELECT add_continuous_aggregate_policy('iot.greenhouse_daily',
    start_offset => INTERVAL '30 days',
    end_offset => INTERVAL '6 hours',
    schedule_interval => INTERVAL '6 hours',
    if_not_exists => TRUE
);

-- ═══════════════════════════════════════════════════════════════════
-- CONTINUOUS AGGREGATE NIVEL 4: MENSUAL (sobre diario)
-- ═══════════════════════════════════════════════════════════════════
CREATE MATERIALIZED VIEW iot.greenhouse_monthly
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 month', bucket) AS bucket,
    greenhouse_id,
    tenant_id,
    
    AVG(avg_temp_01) AS avg_temp_01,
    MIN(min_temp_01) AS min_temp_01,
    MAX(max_temp_01) AS max_temp_01,
    AVG(avg_temp_02) AS avg_temp_02,
    AVG(avg_temp_03) AS avg_temp_03,
    
    AVG(avg_hum_01) AS avg_hum_01,
    
    SUM(total_readings) AS total_readings
FROM iot.greenhouse_daily
GROUP BY bucket, greenhouse_id, tenant_id
WITH NO DATA;

-- Refresh diario
SELECT add_continuous_aggregate_policy('iot.greenhouse_monthly',
    start_offset => INTERVAL '2 years',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);
```

---

### NIVEL 4: POLÍTICAS DE RETENCIÓN AGRESIVAS

```sql
-- ═══════════════════════════════════════════════════════════════════
-- POLÍTICAS DE RETENCIÓN POR NIVEL
-- ═══════════════════════════════════════════════════════════════════

-- NIVEL 1: Datos crudos → ELIMINAR DESPUÉS DE 7 DÍAS
-- (Los datos ya están agregados en hourly)
SELECT add_retention_policy('iot.greenhouse_readings',
    drop_after => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- NIVEL 2: Agregados horarios → ELIMINAR DESPUÉS DE 90 DÍAS
-- (Los datos ya están en daily)
SELECT add_retention_policy('iot.greenhouse_hourly',
    drop_after => INTERVAL '90 days',
    if_not_exists => TRUE
);

-- NIVEL 3: Agregados diarios → ELIMINAR DESPUÉS DE 2 AÑOS
-- (Los datos ya están en monthly)
SELECT add_retention_policy('iot.greenhouse_daily',
    drop_after => INTERVAL '2 years',
    if_not_exists => TRUE
);

-- NIVEL 4: Agregados mensuales → RETENCIÓN INDEFINIDA
-- (Mantener para histórico y cumplimiento)
-- No se agrega política de retención
```

---

## 📈 PROYECCIÓN DE ALMACENAMIENTO

### Escenario Actual (sin optimizar)
```
Frecuencia:       1 mensaje cada ~0.8 segundos (~75/min)
Filas/día:        ~2,400,000 (25 filas × 96,000 mensajes)
Tamaño/día:       ~400 MB sin comprimir, ~40 MB comprimido
Tamaño/mes:       ~1.2 GB comprimido
Tamaño/año:       ~14.4 GB comprimido

⚠️ Con disco de 12 GB, se llenaría en ~8 meses
```

### Escenario Optimizado (con estrategia completa)
```
Frecuencia:       1 mensaje cada 60 segundos (12/min)
Modelo:           1 fila por mensaje (no 25)
Filas/día:        ~17,280 (1 fila × 17,280 mensajes)
Retención:        7 días raw → 90 días hourly → 2 años daily

Tamaño estimado:
  - Raw (7 días):     ~10 MB
  - Hourly (90 días): ~30 MB
  - Daily (2 años):   ~5 MB
  - Monthly (indef):  ~1 MB
  
TOTAL: ~50 MB vs ~1.2 GB actual

✅ Reducción del 96% en almacenamiento
```

---

## 🚀 PLAN DE IMPLEMENTACIÓN

### Fase 1: Emergencia (HOY - 30 minutos)

```bash
# 1. Detener datos duplicados
kubectl scale deployment/invernaderos-api -n apptolast-invernadero-api-prod --replicas=1

# 2. Verificar
kubectl get pods -n apptolast-invernadero-api-prod
# Debe mostrar solo 1 pod Running
```

### Fase 2: Configuración (HOY - 1 hora)

```bash
# 1. Actualizar intervalo de simulación a 60 segundos
# Editar el configmap
kubectl edit configmap invernaderos-api-config -n apptolast-invernadero-api-prod

# Cambiar:
#   interval-ms: 5000
# Por:
#   interval-ms: 60000

# 2. Reiniciar el deployment
kubectl rollout restart deployment/invernaderos-api -n apptolast-invernadero-api-prod
```

### Fase 3: Migración de Esquema (Esta semana)

1. Crear la nueva tabla `greenhouse_readings`
2. Modificar `MqttMessageProcessor` para escribir en el nuevo formato
3. Crear continuous aggregates
4. Configurar políticas de retención
5. Migrar datos históricos (opcional)

### Fase 4: Validación (Próxima semana)

1. Monitorear tamaño de base de datos
2. Verificar que las agregaciones funcionan
3. Confirmar que los dashboards/apps siguen funcionando
4. Ajustar políticas según sea necesario

---

## 🔧 SCRIPT DE IMPLEMENTACIÓN RÁPIDA

```bash
#!/bin/bash
# emergency-fix.sh - Ejecutar AHORA

echo "🚨 EMERGENCIA: Reduciendo duplicación de datos..."

# 1. Reducir a 1 réplica en producción
kubectl scale deployment/invernaderos-api -n apptolast-invernadero-api-prod --replicas=1

# 2. Desactivar simulación en desarrollo
kubectl set env deployment/invernaderos-api -n apptolast-invernadero-api-dev \
  GREENHOUSE_SIMULATION_ENABLED=false

# 3. Verificar
echo "✅ Verificando cambios..."
kubectl get pods -n apptolast-invernadero-api-prod
kubectl get pods -n apptolast-invernadero-api-dev

echo ""
echo "⏳ Esperar 5 minutos y verificar frecuencia de datos..."
echo "   PGPASSWORD='...' psql -h 138.199.157.58 -p 30432 -U admin -d greenhouse_timeseries -c \\"
echo "   \"SELECT COUNT(*) / 5.0 as msgs_per_minute FROM iot.sensor_readings WHERE time >= NOW() - INTERVAL '5 minutes';\""
```

---

## 📋 CHECKLIST DE VALIDACIÓN

- [ ] Solo 1 réplica en producción
- [ ] Simulación desactivada en desarrollo
- [ ] Frecuencia de mensajes ~12/minuto (no ~75/minuto)
- [ ] Intervalo de simulación = 60 segundos
- [ ] Compresión funcionando (chunks comprimidos)
- [ ] Políticas de retención configuradas
- [ ] Alertas de disco configuradas (70%, 85%)

---

## ❓ DECISIONES PENDIENTES

1. **¿Mantener simulación o usar datos reales?**
   - Si hay sensores reales: desactivar simulación completamente
   - Si no hay sensores: mantener simulación con intervalo de 60 seg

2. **¿Retención de datos crudos?**
   - 7 días (recomendado para ahorro máximo)
   - 30 días (si necesitas debugging)
   - 90 días (máximo razonable)

3. **¿Migrar a modelo columnar?**
   - Sí: Requiere cambios en la API
   - No: Mantener modelo actual con mejor configuración

---

## 📞 CONTACTO

Si tienes dudas sobre la implementación, discutamos:
- Prioridad de los cambios
- Impacto en la aplicación móvil
- Necesidades de retención de datos
- Frecuencia óptima de muestreo

**¡ESTO ES CRÍTICO - ACTUAR HOY!**
