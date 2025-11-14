# Guía de Simulación de Datos de Invernadero

## 📋 Descripción General

Este sistema permite generar datos simulados de sensores de invernadero cuando los sensores físicos no están disponibles (por ejemplo, debido a condiciones climáticas adversas como lluvia).

Los datos simulados:
- ✅ Tienen valores realistas (temperaturas, humedades, etc.)
- ✅ Pasan por el mismo flujo que datos reales
- ✅ Se guardan en Redis (cache) y TimescaleDB (persistencia)
- ✅ Se publican vía WebSocket a clientes móviles (iOS, Android, Web, Desktop)
- ✅ Se generan automáticamente cada 5 segundos

---

## 🚀 Cómo Activar la Simulación

### Opción 1: Variable de Entorno (Recomendado para Docker/Kubernetes)

```bash
# Linux/Mac
export GREENHOUSE_SIMULATION_ENABLED=true

# Windows PowerShell
$env:GREENHOUSE_SIMULATION_ENABLED="true"

# Windows CMD
set GREENHOUSE_SIMULATION_ENABLED=true
```

Luego iniciar la aplicación:
```bash
./gradlew bootRun
```

### Opción 2: Modificar application.yaml

Editar `src/main/resources/application.yaml`:

```yaml
greenhouse:
  simulation:
    enabled: true  # Cambiar de false a true
    interval-ms: 5000
    greenhouse-id: "001"
```

### Opción 3: Docker Compose

Agregar al `docker-compose.yaml`:

```yaml
services:
  api:
    environment:
      - GREENHOUSE_SIMULATION_ENABLED=true
```

---

## 📊 Datos Generados

### Temperaturas (°C)
- **Invernadero 01**: 20-28°C (base: 24°C ± 4°C)
- **Invernadero 02**: 18-26°C (base: 22°C ± 4°C)
- **Invernadero 03**: 21-29°C (base: 25°C ± 4°C)

**Distribución**: Gaussiana (más valores cerca de la media, más realista)

### Humedades (%)
- **Invernadero 01**: 50-80% (base: 65% ± 15%)
- **Invernadero 02**: 45-75% (base: 60% ± 15%)
- **Invernadero 03**: 55-85% (base: 70% ± 15%)

**Distribución**: Gaussiana con clamping en 30-90%

### Sectores (% apertura de ventanas)
- **Rango**: 0-100%
- **Comportamiento**:
  - 70% de probabilidad: valores moderados (30-70%)
  - 30% de probabilidad: valores extremos (0% o 100%)

### Extractores (binario)
- **Valores**: 0.0 (apagado) o 1.0 (encendido)
- **Probabilidad**: 30% encendido, 70% apagado

### Reserva
- **Rango**: 0-100 (valor aleatorio uniforme)

---

## 🔍 Verificar que Funciona

### 1. Logs de la Aplicación

Cuando inicies con simulación activada, verás:

```
╔════════════════════════════════════════════════════════════╗
║  SIMULACIÓN DE DATOS DE INVERNADERO ACTIVADA              ║
║                                                            ║
║  Los datos mostrados son SIMULADOS, no provienen de       ║
║  sensores reales. Esto es debido a que los sensores       ║
║  físicos están fuera de servicio.                         ║
║                                                            ║
║  Para desactivar: greenhouse.simulation.enabled=false     ║
╚════════════════════════════════════════════════════════════╝
```

Y cada 5 segundos:

```
========================================
INICIANDO SIMULACIÓN DE DATOS DE INVERNADERO
Generando datos cada 5 segundos
Greenhouse ID: 001
========================================

Datos simulados procesados exitosamente - Temp01: 23.45°C, Hum01: 67.23%
```

### 2. WebSocket (Cliente Móvil)

Tu aplicación Kotlin Multiplatform recibirá datos en el topic STOMP:

```
/topic/greenhouse/messages
```

Estructura recibida:
```json
{
  "timestamp": "2025-01-14T12:34:56.789Z",
  "TEMPERATURA INVERNADERO 01": 23.45,
  "HUMEDAD INVERNADERO 01": 67.23,
  "TEMPERATURA INVERNADERO 02": 21.87,
  "HUMEDAD INVERNADERO 02": 59.12,
  "TEMPERATURA INVERNADERO 03": 24.56,
  "HUMEDAD INVERNADERO 03": 71.34,
  "INVERNADERO_01_SECTOR_01": 45.67,
  "INVERNADERO_01_SECTOR_02": 52.34,
  "INVERNADERO_01_SECTOR_03": 38.91,
  "INVERNADERO_01_SECTOR_04": 61.23,
  "INVERNADERO_02_SECTOR_01": 0.0,
  "INVERNADERO_02_SECTOR_02": 100.0,
  "INVERNADERO_02_SECTOR_03": 47.89,
  "INVERNADERO_02_SECTOR_04": 53.45,
  "INVERNADERO_03_SECTOR_01": 42.11,
  "INVERNADERO_03_SECTOR_02": 58.76,
  "INVERNADERO_03_SECTOR_03": 0.0,
  "INVERNADERO_03_SECTOR_04": 55.32,
  "INVERNADERO_01_EXTRACTOR": 1.0,
  "INVERNADERO_02_EXTRACTOR": 0.0,
  "INVERNADERO_03_EXTRACTOR": 0.0,
  "RESERVA": 73.21,
  "greenhouseId": "001"
}
```

### 3. API REST (Testing Manual)

#### Ver estado de simulación
```bash
curl http://localhost:8080/api/simulation/status
```

Respuesta:
```json
{
  "enabled": true,
  "schedulerActive": true,
  "intervalMs": 5000,
  "greenhouseId": "001",
  "message": "Simulación activa - Generando datos cada 5 segundos"
}
```

#### Generar datos bajo demanda
```bash
curl -X POST http://localhost:8080/api/simulation/generate?greenhouseId=001
```

#### Ver preview sin guardar
```bash
curl http://localhost:8080/api/simulation/preview?greenhouseId=001
```

#### Información de la API
```bash
curl http://localhost:8080/api/simulation/info
```

### 4. Swagger UI

Accede a: `http://localhost:8080/swagger-ui.html`

Busca la sección **"Simulation"** para probar los endpoints interactivamente.

### 5. Verificar en Base de Datos

**Redis Cache:**
```bash
redis-cli -h 138.199.157.58 -p 30379 -a AppToLast2023%
ZRANGE greenhouse:messages 0 -1
ZREVRANGE greenhouse:messages 0 9 WITHSCORES
```

**TimescaleDB:**
```sql
-- Conectar a TimescaleDB
psql -h 138.199.157.58 -p 30432 -U admin -d greenhouse_timeseries

-- Ver últimas lecturas
SELECT time, sensor_id, greenhouse_id, value, unit
FROM sensor_reading
WHERE greenhouse_id = '001'
ORDER BY time DESC
LIMIT 50;

-- Contar lecturas por hora
SELECT time_bucket('1 hour', time) as hour,
       COUNT(*) as readings
FROM sensor_reading
WHERE greenhouse_id = '001'
GROUP BY hour
ORDER BY hour DESC;
```

---

## 🎯 Casos de Uso

### Caso 1: Sensores Estropeados por Lluvia (Tu caso actual)
1. Activar simulación: `GREENHOUSE_SIMULATION_ENABLED=true`
2. Iniciar aplicación
3. La app móvil recibirá datos simulados cada 5 segundos
4. Los datos se verán como datos reales en toda la infraestructura

### Caso 2: Testing de App Móvil
```bash
# Terminal 1: Iniciar API con simulación
GREENHOUSE_SIMULATION_ENABLED=true ./gradlew bootRun

# Terminal 2: Generar datos adicionales bajo demanda
curl -X POST http://localhost:8080/api/simulation/generate
```

### Caso 3: Demo para Cliente
1. Activar simulación antes de la demo
2. Los datos fluyen automáticamente cada 5 segundos
3. El cliente ve el sistema funcionando aunque no haya sensores reales

### Caso 4: Development/Debugging
```bash
# Solo preview (no guarda en BD)
curl http://localhost:8080/api/simulation/preview

# Ver qué datos se generarían sin contaminar la base de datos
```

---

## ⚙️ Configuración Avanzada

### Cambiar Intervalo de Generación

Editar `application.yaml`:
```yaml
greenhouse:
  simulation:
    interval-ms: 3000  # Generar cada 3 segundos
```

### Cambiar Greenhouse ID

```yaml
greenhouse:
  simulation:
    greenhouse-id: "002"  # Simular otro invernadero
```

### Ajustar Rangos de Valores

Editar `GreenhouseDataSimulator.kt`:

```kotlin
// Cambiar temperatura base del invernadero 01
temperaturaInvernadero01 = generateTemperature(baseTemp = 26.0, variation = 3.0)

// Cambiar rango de humedad
humedadInvernadero01 = generateHumidity(baseHumidity = 70.0, variation = 10.0)
```

---

## 🛑 Desactivar Simulación

### Opción 1: Variable de entorno
```bash
export GREENHOUSE_SIMULATION_ENABLED=false
```

### Opción 2: application.yaml
```yaml
greenhouse:
  simulation:
    enabled: false
```

### Opción 3: Eliminar variable (usa default)
```bash
unset GREENHOUSE_SIMULATION_ENABLED
# El valor por defecto es 'false'
```

---

## 📁 Archivos Creados

```
src/main/kotlin/com/apptolast/invernaderos/
├── service/
│   └── GreenhouseDataSimulator.kt          # Generador de datos realistas
├── scheduler/
│   └── GreenhouseSimulationScheduler.kt    # Scheduler automático (cada 5s)
├── controllers/
│   └── SimulationController.kt             # API REST para control manual
└── InvernaderosApplication.kt              # Actualizado con @EnableScheduling

src/main/resources/
└── application.yaml                        # Configuración de simulación añadida
```

---

## 🔧 Troubleshooting

### La simulación no se activa

**Verificar:**
1. ✅ `greenhouse.simulation.enabled=true` en application.yaml
2. ✅ O `GREENHOUSE_SIMULATION_ENABLED=true` como variable de entorno
3. ✅ `@EnableScheduling` en `InvernaderosApplication.kt`
4. ✅ Reiniciar la aplicación después de cambios

**Ver logs:**
```bash
# Debe aparecer el banner de "SIMULACIÓN ACTIVADA"
./gradlew bootRun | grep -A 10 "SIMULACIÓN"
```

### No aparecen datos en WebSocket

**Verificar:**
1. ✅ Cliente conectado al WebSocket: `ws://host:8080/ws/greenhouse`
2. ✅ Suscrito al topic correcto: `/topic/greenhouse/messages`
3. ✅ Ver logs del `GreenhouseWebSocketHandler`:
   ```
   Evento recibido, transmitiendo via WebSocket
   Mensaje transmitido exitosamente via WebSocket
   ```

### Endpoints REST no disponibles (404)

**Causa**: El `SimulationController` solo se crea si `greenhouse.simulation.enabled=true`

**Solución**: Activar simulación primero, luego los endpoints estarán disponibles.

### Demasiados datos en la base de datos

**Solución 1**: Reducir intervalo
```yaml
interval-ms: 10000  # Cada 10 segundos en lugar de 5
```

**Solución 2**: Usar solo preview (no guarda en BD)
```bash
curl http://localhost:8080/api/simulation/preview
```

**Solución 3**: Limpiar datos antiguos
```sql
DELETE FROM sensor_reading
WHERE greenhouse_id = '001'
  AND time < NOW() - INTERVAL '1 day';
```

---

## 📝 Notas Importantes

1. **Producción**: Mantener `enabled: false` cuando los sensores reales funcionen
2. **Datos realistas**: La distribución gaussiana crea patrones más naturales
3. **Mismo flujo**: Los datos simulados pasan por Redis → TimescaleDB → WebSocket
4. **Performance**: Generar datos cada 5 segundos es eficiente y realista
5. **Extensibilidad**: Fácil ajustar rangos editando `GreenhouseDataSimulator.kt`

---

## 🎓 Arquitectura de la Solución

```
┌─────────────────────────────────────────────────────────────┐
│  GreenhouseSimulationScheduler (cada 5 segundos)            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ↓
         ┌────────────────────────────────┐
         │  GreenhouseDataSimulator       │
         │  - generateRealisticData()     │
         │  - Distribución gaussiana      │
         │  - Rangos realistas            │
         └────────────────┬───────────────┘
                          │
                          ↓ RealDataDto
         ┌────────────────────────────────┐
         │  RealDataDto.toJson()          │
         │  (con @JsonProperty mapping)   │
         └────────────────┬───────────────┘
                          │
                          ↓ JSON String
         ┌────────────────────────────────┐
         │  MqttMessageProcessor          │
         │  .processGreenhouseData()      │
         └─────────────┬──────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
    ┌──────┐     ┌──────────┐   ┌─────────┐
    │ Redis│     │TimescaleDB   │ Event   │
    │Cache │     │  Batch Save  │Publisher│
    └──────┘     └──────────┘   └────┬────┘
                                      │
                                      ↓
                          ┌───────────────────────┐
                          │GreenhouseWebSocket    │
                          │Handler                │
                          │@EventListener         │
                          └──────────┬────────────┘
                                     │
                                     ↓ SimpMessagingTemplate
                          ┌───────────────────────┐
                          │ /topic/greenhouse/    │
                          │ messages              │
                          └──────────┬────────────┘
                                     │
                                     ↓
                    ┌────────────────────────────────┐
                    │  Clientes WebSocket/STOMP      │
                    │  - iOS App                     │
                    │  - Android App                 │
                    │  - Web App                     │
                    │  - Desktop App                 │
                    └────────────────────────────────┘
```

---

## ✅ Checklist de Implementación Completada

- [x] `GreenhouseDataSimulator.kt` - Generador con distribución gaussiana
- [x] `GreenhouseSimulationScheduler.kt` - Scheduler cada 5 segundos
- [x] `SimulationController.kt` - API REST para control
- [x] `application.yaml` - Configuración añadida
- [x] `@EnableScheduling` - Habilitado en aplicación principal
- [x] Documentación - Esta guía completa
- [x] Integración - Usa flujo existente (Redis + DB + WebSocket)
- [x] Testing - Endpoints REST para pruebas manuales

---

**¡Todo listo!** 🎉

Para activar la simulación ahora mismo:

```bash
export GREENHOUSE_SIMULATION_ENABLED=true
./gradlew bootRun
```

Y tu app móvil empezará a recibir datos simulados cada 5 segundos a través del WebSocket.
