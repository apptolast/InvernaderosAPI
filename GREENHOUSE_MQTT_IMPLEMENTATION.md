# Implementación MQTT GREENHOUSE - Documentación

## 📋 Resumen

Se ha implementado una solución completa para recibir, almacenar y exponer los mensajes MQTT del topic **GREENHOUSE** siguiendo las mejores prácticas de Spring Boot 2025 con separación de responsabilidades.

## 🏗️ Arquitectura

```
EMQX Broker (WSS)
    ↓
MqttConfig (Spring Integration)
    ↓
GreenhouseDataListener
    ↓
MqttMessageProcessor
    ├─→ Redis Cache (últimos 1000 mensajes)
    ├─→ TimescaleDB (histórico permanente)
    └─→ Spring Event (GreenhouseMessageEvent)
         ↓
    WebSocket Handler → Clientes WebSocket
```

## 📦 Componentes Implementados

### 1. **DTOs** (`entities/dtos/`)
- `GreenhouseMessageDto.kt` - Mensaje GREENHOUSE con SENSOR_XX y SETPOINT_XX
- `GreenhouseStatisticsDto.kt` - Estadísticas (min/max/avg/count)
- `GreenhouseSummaryDto.kt` - Resumen de todos los sensores
- `GreenhouseExtensions.kt` - Extension functions para conversión

### 2. **Servicios** (`service/`)
- `GreenhouseCacheService.kt` - Gestión de caché Redis (Sorted Set)
- `GreenhouseDataService.kt` - Lógica de negocio (coordina Redis + TimescaleDB)

### 3. **Procesamiento MQTT** (`mqtt/service/`)
- `MqttMessageProcessor.kt` - Actualizado para:
  - Cachear en Redis
  - Guardar en TimescaleDB
  - Publicar eventos Spring

### 4. **REST Controller** (`controllers/`)
- `GreenhouseController.kt` - Endpoints REST para consultar mensajes

### 5. **WebSocket** (`websocket/` y `config/`)
- `WebSocketConfig.kt` - Configuración STOMP
- `GreenhouseWebSocketHandler.kt` - Broadcast en tiempo real

### 6. **Configuración**
- `application.yaml` - Broker actualizado a WSS
- `MqttConfig.kt` - Compatible con WebSocket (sin cambios necesarios)

## 🔌 Conexión MQTT

### Broker Configurado
```yaml
URL: ${MQTT_BROKER_URL}  # Configurar en .env o variables de entorno
Username: ${MQTT_USERNAME}  # Configurar en .env o variables de entorno
Password: ${MQTT_PASSWORD}  # Configurar en .env o variables de entorno
Topic: GREENHOUSE
QoS: 0
```

**IMPORTANTE:** Las credenciales MQTT deben configurarse mediante variables de entorno. 
Ver el archivo `.env.example` para más detalles.

### Formato de Mensaje
```json
{
  "SENSOR_01": 1.23,
  "SENSOR_02": 2.23,
  "SETPOINT_01": 0.1,
  "SETPOINT_02": 0.2,
  "SETPOINT_03": 0.3
}
```

## 🌐 Endpoints REST

### 1. Obtener Mensajes Recientes
```http
GET /api/greenhouse/messages/recent?limit=100
```

**Respuesta:**
```json
[
  {
    "timestamp": "2025-11-09T18:16:24Z",
    "sensor01": 1.23,
    "sensor02": 2.23,
    "setpoint01": 0.1,
    "setpoint02": 0.2,
    "setpoint03": 0.3,
    "greenhouseId": "001",
    "rawPayload": "{...}"
  }
]
```

### 2. Obtener Mensajes por Rango de Tiempo
```http
GET /api/greenhouse/messages/range?from=2025-11-09T10:00:00Z&to=2025-11-09T11:00:00Z
```

### 3. Obtener Último Mensaje
```http
GET /api/greenhouse/messages/latest
```

### 4. Estadísticas de un Sensor
```http
GET /api/greenhouse/statistics/SENSOR_01?period=1h
```

**Parámetros de periodo:**
- `1h` - Última hora
- `24h` - Últimas 24 horas
- `7d` - Últimos 7 días
- `30d` - Últimos 30 días

**Respuesta:**
```json
{
  "sensorId": "SENSOR_01",
  "sensorType": "SENSOR",
  "min": 0.5,
  "max": 2.5,
  "avg": 1.5,
  "count": 150,
  "lastValue": 1.23,
  "lastTimestamp": "2025-11-09T18:16:24Z",
  "periodStart": "2025-11-09T17:16:24Z",
  "periodEnd": "2025-11-09T18:16:24Z"
}
```

### 5. Resumen de Estadísticas
```http
GET /api/greenhouse/statistics/summary?period=1h
```

**Respuesta:**
```json
{
  "timestamp": "2025-11-09T18:16:24Z",
  "totalMessages": 500,
  "sensors": {
    "SENSOR_01": {
      "current": 1.23,
      "min": 0.5,
      "max": 2.5,
      "avg": 1.5,
      "count": 500
    },
    "SENSOR_02": { ... }
  },
  "setpoints": {
    "SETPOINT_01": { ... },
    "SETPOINT_02": { ... },
    "SETPOINT_03": { ... }
  },
  "periodStart": "2025-11-09T17:16:24Z",
  "periodEnd": "2025-11-09T18:16:24Z"
}
```

### 6. Información del Caché
```http
GET /api/greenhouse/cache/info
```

**Respuesta:**
```json
{
  "totalMessages": 1000,
  "ttlSeconds": 86400,
  "hasOldestMessage": true,
  "hasLatestMessage": true,
  "maxCapacity": 1000
}
```

### 7. Health Check
```http
GET /api/greenhouse/health
```

## 🔄 WebSocket (Tiempo Real)

### Conexión

**Endpoint WebSocket:**
```
ws://localhost:8080/ws/greenhouse
```

**Con SockJS (compatibilidad navegadores):**
```
http://localhost:8080/ws/greenhouse
```

### Topics STOMP

- `/topic/greenhouse/messages` - Mensajes nuevos en tiempo real
- `/topic/greenhouse/statistics` - Actualizaciones de estadísticas

### Ejemplo JavaScript (Browser)

```javascript
// Usando SockJS + STOMP
const socket = new SockJS('http://localhost:8080/ws/greenhouse');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Conectado al WebSocket');

  // Suscribirse a mensajes nuevos
  stompClient.subscribe('/topic/greenhouse/messages', function(message) {
    const data = JSON.parse(message.body);
    console.log('Nuevo mensaje GREENHOUSE:', data);

    // Actualizar UI con los nuevos datos
    updateSensorDisplay(data);
  });
});

function updateSensorDisplay(data) {
  document.getElementById('sensor01').textContent = data.sensor01;
  document.getElementById('sensor02').textContent = data.sensor02;
  document.getElementById('setpoint01').textContent = data.setpoint01;
  // ... etc
}
```

### Ejemplo con WebSocket Nativo

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/greenhouse-native');

ws.onopen = function() {
  console.log('WebSocket conectado');
};

ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Mensaje recibido:', data);
};
```

## 🗄️ Almacenamiento

### Redis (Caché)
- **Estructura:** Sorted Set con timestamp como score
- **Capacidad:** Últimos 1000 mensajes
- **TTL:** 24 horas
- **Key:** `greenhouse:messages`

### TimescaleDB (Histórico)
- **Tabla:** `sensor_readings`
- **Esquema:** `public`
- **Retention:** Permanente (o según política TimescaleDB)
- **Campos:** time, greenhouseId, sensorId, sensorType, value, unit

## 🚀 Iniciar la Aplicación

```bash
# Compilar
./gradlew build

# Ejecutar
./gradlew bootRun

# O con JAR
java -jar build/libs/invernaderos-0.0.1-SNAPSHOT.jar
```

## 🧪 Probar la Implementación

### 1. Verificar Conexión MQTT
Revisa los logs al iniciar:
```
📥 Mensaje MQTT recibido:
   Topic: GREENHOUSE
   QoS: 0
   Payload: {"SENSOR_01":1.23,...}
```

### 2. Probar REST API
```bash
# Obtener últimos 10 mensajes
curl http://localhost:8080/api/greenhouse/messages/recent?limit=10

# Obtener último mensaje
curl http://localhost:8080/api/greenhouse/messages/latest

# Estadísticas de SENSOR_01 en última hora
curl "http://localhost:8080/api/greenhouse/statistics/SENSOR_01?period=1h"

# Resumen completo
curl "http://localhost:8080/api/greenhouse/statistics/summary?period=24h"
```

### 3. Probar WebSocket

Puedes usar una página HTML simple:

```html
<!DOCTYPE html>
<html>
<head>
  <title>Greenhouse Monitor</title>
  <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
  <h1>Greenhouse Real-Time Monitor</h1>
  <div id="data"></div>

  <script>
    const socket = new SockJS('http://localhost:8080/ws/greenhouse');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
      console.log('Connected: ' + frame);

      stompClient.subscribe('/topic/greenhouse/messages', function(message) {
        const data = JSON.parse(message.body);
        document.getElementById('data').innerHTML =
          '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
      });
    });
  </script>
</body>
</html>
```

## 📊 Flujo de Datos Completo

1. **Mensaje llega al broker EMQX** (topic: GREENHOUSE)
2. **Spring Integration MQTT** recibe el mensaje
3. **GreenhouseDataListener** captura el mensaje
4. **MqttMessageProcessor** procesa:
   - Convierte JSON a `GreenhouseMessageDto`
   - Cachea en Redis (últimos 1000)
   - Guarda en TimescaleDB (permanente)
   - Publica `GreenhouseMessageEvent`
5. **GreenhouseWebSocketHandler** escucha el evento
6. **Broadcast WebSocket** a todos los clientes suscritos
7. **Clientes reciben** datos en tiempo real

## 🔧 Configuración de Producción

### Variables de Entorno Recomendadas

**IMPORTANTE:** NO incluyas las credenciales reales aquí. Usa un gestor de secretos como:
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Kubernetes Secrets

```bash
# MQTT
MQTT_BROKER_URL=wss://your-mqtt-broker.com:443/mqtt
MQTT_USERNAME=<configure_in_secrets_manager>
MQTT_PASSWORD=<configure_in_secrets_manager>

# Redis
REDIS_HOST=<your-redis-host>
REDIS_PORT=<your-redis-port>
REDIS_PASSWORD=<configure_in_secrets_manager>

# TimescaleDB
TIMESCALE_PASSWORD=<configure_in_secrets_manager>

# Metadata DB
METADATA_PASSWORD=<configure_in_secrets_manager>
```

### CORS en Producción

Editar `GreenhouseController.kt`:
```kotlin
@CrossOrigin(origins = ["https://tu-dominio.com"])
```

Y `WebSocketConfig.kt`:
```kotlin
registry.addEndpoint("/ws/greenhouse")
    .setAllowedOrigins("https://tu-dominio.com")
    .withSockJS()
```

## 📈 Monitoreo

### Logs Importantes
```yaml
logging:
  level:
    com.apptolast.invernaderos: DEBUG
    org.springframework.integration.mqtt: INFO
```

### Métricas Disponibles
- Total mensajes en cache: `/api/greenhouse/cache/info`
- Health check: `/api/greenhouse/health`
- Actuator: `/actuator/health`

## 🎯 Características Implementadas

✅ Conexión al broker EMQX via WebSocket (WSS)
✅ Procesamiento de mensajes GREENHOUSE con formato exacto
✅ Caché Redis con los últimos 1000 mensajes
✅ Persistencia permanente en TimescaleDB
✅ REST API completa con 7 endpoints
✅ WebSocket en tiempo real (STOMP)
✅ Estadísticas agregadas (min/max/avg)
✅ Filtrado por rango de tiempo
✅ Separación de responsabilidades (Controller → Service → Repository/Cache)
✅ Event-driven architecture (Spring Events)
✅ Logging estructurado (SLF4J)
✅ Extension functions para conversión de DTOs
✅ Validación de parámetros
✅ Manejo de errores robusto

## 🔍 Troubleshooting

### Problema: No recibo mensajes MQTT
- Verificar logs: buscar "📥 Mensaje MQTT recibido"
- Verificar conexión al broker: logs de Spring Integration MQTT
- Verificar credenciales en `application.yaml`

### Problema: WebSocket no conecta
- Verificar puerto (default: 8080)
- Verificar CORS si accedes desde otro dominio
- Probar endpoint: `http://localhost:8080/ws/greenhouse`

### Problema: No hay datos en Redis
- Verificar conexión Redis: logs de Lettuce
- Verificar que `GreenhouseCacheService` se está inyectando
- Usar endpoint: `/api/greenhouse/cache/info`

### Problema: Queries lentas en TimescaleDB
- Crear índices en `time` y `sensorId`
- Usar políticas de retention de TimescaleDB
- Considerar compresión de datos antiguos

## 📚 Referencias

- [Spring Integration MQTT](https://docs.spring.io/spring-integration/reference/mqtt.html)
- [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [TimescaleDB Best Practices](https://docs.timescale.com/use-timescale/latest/)
- [Redis Sorted Sets](https://redis.io/docs/data-types/sorted-sets/)

---

**Implementado:** 2025-11-09
**Versión:** Spring Boot 3.5.7 + Kotlin 1.9.25
**Arquitectura:** Event-Driven + REST + WebSocket + Redis Cache + TimescaleDB
