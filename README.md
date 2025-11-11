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
5. **Ejecuta validación de seguridad:** `./scripts/validate-security.sh`
6. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
7. Push a la rama (`git push origin feature/AmazingFeature`)
8. Abre un Pull Request

## 📝 Licencia

Este proyecto es privado y propiedad de AppToLast.

## 🆘 Soporte

Para reportar problemas o solicitar ayuda:
- Crear un issue en: https://github.com/apptolast/InvernaderosAPI/issues
- Contactar al equipo de desarrollo

---

**Construido con ❤️ usando Spring Boot, Kotlin y mejores prácticas de seguridad**
