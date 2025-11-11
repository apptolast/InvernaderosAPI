# InvernaderosAPI 🌱

API REST para gestión de invernaderos IoT con monitoreo en tiempo real de sensores mediante MQTT, almacenamiento de series temporales y cache distribuido.

## 🚀 Características

- ✅ API REST con Spring Boot 3.5.7 + Kotlin
- ✅ Base de datos de series temporales con TimescaleDB
- ✅ Base de datos metadata con PostgreSQL
- ✅ Cache distribuido con Redis
- ✅ Comunicación MQTT con EMQX Broker
- ✅ WebSocket para actualizaciones en tiempo real
- ✅ Documentación OpenAPI/Swagger
- ✅ Containerización con Docker

## 📋 Requisitos

- Docker Desktop o Docker Engine (v20.10+)
- Docker Compose (v2.0+)
- Java 21 JDK (opcional, para desarrollo local)
- Git

## 🔧 Configuración Inicial

### 1. Clonar el Repositorio

```bash
git clone https://github.com/apptolast/InvernaderosAPI.git
cd InvernaderosAPI
```

### 2. Configurar Variables de Entorno

**IMPORTANTE:** Este proyecto requiere configuración de credenciales mediante variables de entorno.

```bash
# Copiar el archivo de ejemplo
cp .env.example .env
```

Edita el archivo `.env` y configura las credenciales:

```bash
# Generar passwords seguros
openssl rand -base64 32
```

Reemplaza todos los valores `<your_*>` con tus credenciales reales.

**⚠️ NUNCA subas el archivo `.env` a Git**

### 3. Configurar el Archivo de Aplicación

```bash
cp application-local.yaml.example application-local.yaml
```

Este archivo también está protegido por `.gitignore`.

### 4. Iniciar los Servicios

```bash
# Levantar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f api
```

### 5. Verificar que Todo Funciona

```bash
# Health check
curl http://localhost:8080/actuator/health

# Abrir Swagger UI
open http://localhost:8080/swagger-ui.html
```

## 🌐 Servicios Disponibles

| Servicio | Puerto | URL |
|----------|--------|-----|
| API REST | 8080 | http://localhost:8080 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |
| TimescaleDB | 5432 | localhost:5432 |
| PostgreSQL Metadata | 5433 | localhost:5433 |
| Redis | 6379 | localhost:6379 |
| EMQX Dashboard | 18083 | http://localhost:18083 |
| EMQX MQTT | 1883 | tcp://localhost:1883 |

## 📚 Documentación

- [Guía de Despliegue](DEPLOYMENT.md) - Instrucciones detalladas de despliegue
- [Implementación MQTT](GREENHOUSE_MQTT_IMPLEMENTATION.md) - Documentación MQTT y WebSocket
- [Guía de Seguridad](SECURITY.md) - Mejores prácticas de seguridad

## 🔐 Seguridad

Este proyecto sigue las mejores prácticas de seguridad:

- ✅ Sin credenciales hardcodeadas en el código
- ✅ Variables de entorno para todas las credenciales
- ✅ Archivos sensibles en `.gitignore`
- ✅ CodeQL security scanning
- ✅ Dependabot para vulnerabilidades

**Ver [SECURITY.md](SECURITY.md) para más detalles**

## 🏗️ Estructura del Proyecto

```
InvernaderosAPI/
├── src/
│   ├── main/
│   │   └── kotlin/com/apptolast/invernaderos/
│   │       ├── config/          # Configuración
│   │       ├── controllers/     # REST Controllers
│   │       ├── entities/        # Entidades y DTOs
│   │       ├── repositories/    # Repositorios
│   │       ├── services/        # Lógica de negocio
│   │       ├── mqtt/           # MQTT listeners y publishers
│   │       └── websocket/      # WebSocket handlers
│   └── test/                   # Tests
├── docker-compose.yaml         # Orquestación de servicios
├── Dockerfile                  # Imagen de la aplicación
├── .env.example               # Template de variables de entorno
├── application-local.yaml.example  # Template de configuración
└── README.md                  # Este archivo
```

## 🧪 Testing

```bash
# Ejecutar tests
./gradlew test

# Ejecutar tests con coverage
./gradlew test jacocoTestReport
```

## 🔨 Build

```bash
# Build local
./gradlew build

# Build sin tests (más rápido)
./gradlew build -x test

# Ejecutar la aplicación
./gradlew bootRun
```

## 🐳 Docker

```bash
# Build imagen
docker build -t invernaderos-api .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e TIMESCALE_PASSWORD=your_password \
  -e METADATA_PASSWORD=your_password \
  -e REDIS_PASSWORD=your_password \
  -e MQTT_USERNAME=your_username \
  -e MQTT_PASSWORD=your_password \
  invernaderos-api
```

## 📊 API Endpoints

### Health Check
```http
GET /actuator/health
```

### Greenhouse Messages
```http
GET /api/greenhouse/messages/recent?limit=100
GET /api/greenhouse/messages/latest
GET /api/greenhouse/messages/range?from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z
```

### Statistics
```http
GET /api/greenhouse/statistics/SENSOR_01?period=1h
GET /api/greenhouse/statistics/summary?period=24h
```

Ver [GREENHOUSE_MQTT_IMPLEMENTATION.md](GREENHOUSE_MQTT_IMPLEMENTATION.md) para más detalles.

## 🔄 WebSocket

Conectar al WebSocket para recibir datos en tiempo real:

```javascript
const socket = new SockJS('http://localhost:8080/ws/greenhouse');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  stompClient.subscribe('/topic/greenhouse/messages', function(message) {
    const data = JSON.parse(message.body);
    console.log('Nuevo mensaje:', data);
  });
});
```

Ver [greenhouse-client-demo.html](greenhouse-client-demo.html) para un ejemplo completo.

## 🚀 CI/CD

El proyecto usa GitHub Actions para CI/CD:

- **Build y Tests:** Se ejecutan en cada PR
- **Docker Build:** Se construye imagen en push a `main` o `develop`
- **Security Scanning:** CodeQL analiza el código automáticamente
# 🌱 Invernaderos API

API para monitoreo y control de invernaderos inteligentes usando Spring Boot, MQTT, TimescaleDB y Redis.

## 🚀 Inicio Rápido

### Prerrequisitos

- Docker y Docker Compose
- Java 21 JDK (opcional, para desarrollo sin Docker)
- Git

### Configuración Inicial

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/apptolast/InvernaderosAPI.git
   cd InvernaderosAPI
   ```

2. **Configurar credenciales:**
   ```bash
   # Copiar archivos de ejemplo
   cp .env.example .env
   cp docker-compose.override.yaml.example docker-compose.override.yaml
   cp application-local.yaml.example application-local.yaml
   
   # Editar .env y configurar contraseñas seguras
   nano .env
   ```

   **⚠️ IMPORTANTE:** Nunca uses contraseñas por defecto. Genera contraseñas únicas y seguras para cada servicio.

3. **Levantar los servicios:**
   ```bash
   docker-compose up -d
   ```

4. **Verificar que funciona:**
   ```bash
   # Health check
   curl http://localhost:8080/actuator/health
   
   # Abrir Swagger UI
   open http://localhost:8080/swagger-ui.html
   ```

## 📚 Documentación

- [📖 Guía de Despliegue](DEPLOYMENT.md) - Instrucciones completas de despliegue
- [🔒 Guía de Seguridad](SECURITY.md) - Gestión segura de credenciales
- [📡 Implementación MQTT](GREENHOUSE_MQTT_IMPLEMENTATION.md) - Detalles de integración MQTT
- [🔍 Reporte de Auditoría](SECURITY_AUDIT_REPORT.md) - Auditoría de seguridad realizada

## 🏗️ Arquitectura

```
Cliente MQTT/WebSocket
        ↓
   Spring Boot API
   ├─→ EMQX (MQTT Broker)
   ├─→ Redis (Cache)
   ├─→ TimescaleDB (Series temporales)
   └─→ PostgreSQL (Metadata)
```

## 🔐 Seguridad

Este proyecto implementa las mejores prácticas de seguridad:

- ✅ Sin credenciales hardcodeadas
- ✅ Variables de entorno para todos los secretos
- ✅ Archivos sensibles en `.gitignore`
- ✅ Documentación de gestión segura de credenciales

Ver [SECURITY.md](SECURITY.md) para más detalles.

### Validación de Seguridad

Antes de hacer commit, ejecuta:

```bash
./scripts/validate-security.sh
```

## 🛠️ Desarrollo

### Compilar

```bash
./gradlew build
```

### Ejecutar tests

```bash
./gradlew test
```

### Ejecutar localmente

```bash
./gradlew bootRun
```

## 📡 Endpoints Principales

- **API REST:** `http://localhost:8080/api`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **WebSocket:** `ws://localhost:8080/ws/greenhouse`
- **Health Check:** `http://localhost:8080/actuator/health`

## 🐳 Servicios Docker

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| API | 8080 | Spring Boot API |
| TimescaleDB | 5432 | Base de datos de series temporales |
| PostgreSQL | 5433 | Base de datos de metadata |
| Redis | 6379 | Cache |
| EMQX | 1883 | MQTT Broker |
| EMQX Dashboard | 18083 | Dashboard de EMQX |

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Configura tus credenciales locales (ver `.env.example`)
4. Haz tus cambios
5. **Antes de hacer commit:**
   - Verifica que no hay credenciales expuestas
   - Ejecuta los tests
   - Sigue las guías de seguridad en [SECURITY.md](SECURITY.md)
   - **Ejecuta validación de seguridad:** `./scripts/validate-security.sh`
6. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
7. Push a la rama (`git push origin feature/AmazingFeature`)
8. Abre un Pull Request

## 📝 Licencia

Este proyecto es privado y propiedad de AppToLast.

## 📧 Contacto

AppToLast - info@apptolast.com

Project Link: [https://github.com/apptolast/InvernaderosAPI](https://github.com/apptolast/InvernaderosAPI)

## 🙏 Agradecimientos

- Spring Boot
- Kotlin
- TimescaleDB
- EMQX
- Redis
- PostgreSQL

---

**Nota:** Para problemas de seguridad, por favor lee [SECURITY.md](SECURITY.md) antes de reportar.
## 🆘 Soporte

Para reportar problemas o solicitar ayuda:
- Crear un issue en: https://github.com/apptolast/InvernaderosAPI/issues
- Contactar al equipo de desarrollo

---

**Construido con ❤️ usando Spring Boot, Kotlin y mejores prácticas de seguridad**
