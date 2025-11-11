# Guía de Despliegue - Invernaderos API

Esta guía detalla cómo desplegar la API de Invernaderos en los diferentes entornos: desarrollo local, desarrollo en Kubernetes y producción.

## Tabla de Contenidos

- [Arquitectura de Despliegue](#arquitectura-de-despliegue)
- [Prerrequisitos](#prerrequisitos)
- [Desarrollo Local con Docker Compose](#desarrollo-local-con-docker-compose)
- [Despliegue en Kubernetes - Desarrollo](#despliegue-en-kubernetes---desarrollo)
- [Despliegue en Kubernetes - Producción](#despliegue-en-kubernetes---producción)
- [CI/CD con GitHub Actions](#cicd-con-github-actions)
- [Verificación y Troubleshooting](#verificación-y-troubleshooting)

---

## Arquitectura de Despliegue

### Entornos

| Entorno | Rama Git | Namespace K8s | Dominio | Imagen Docker |
|---------|----------|---------------|---------|---------------|
| **Local** | - | - | localhost:8080 | Build local |
| **Desarrollo** | `develop` | `apptolast-invernadero-api-dev` | inverapi-dev.apptolast.com | `apptolast/invernaderos-api:develop` |
| **Producción** | `main` | `apptolast-invernadero-api-prod` | inverapi-prod.apptolast.com | `apptolast/invernaderos-api:latest` |

### Infraestructura Compartida

Los entornos de desarrollo y producción comparten la siguiente infraestructura en el namespace `apptolast-invernadero-api`:

- **TimescaleDB** (puerto interno: 5432)
- **PostgreSQL Metadata** (puerto interno: 5432)
- **Redis** (puerto interno: 6379)
- **EMQX MQTT Broker** (WSS: mqttinvernaderoapi-ws.apptolast.com:443)

**Separación de datos:**
- **Producción:** Usa `greenhouse_timeseries` y `greenhouse_metadata`, Redis database 0
- **Desarrollo:** Usa `greenhouse_timeseries_dev` y `greenhouse_metadata_dev`, Redis database 1

---

## Prerrequisitos

### Para desarrollo local:

- Docker Desktop o Docker Engine (v20.10+)
- Docker Compose (v2.0+)
- Java 21 JDK (opcional, para desarrollo sin Docker)
- Gradle 8.14+ (incluido via wrapper)

### Para Kubernetes:

- kubectl instalado y configurado
- Acceso al cluster de Kubernetes
- cert-manager instalado en el cluster
- Traefik como Ingress Controller
- Credenciales de DockerHub configuradas en GitHub Secrets

---

## Desarrollo Local con Docker Compose

### Paso 1: Crear archivo de configuración

Copia el archivo de ejemplo y ajústalo según necesites:

```bash
cp application-local.yaml.example application-local.yaml
```

**IMPORTANTE:** El archivo `application-local.yaml` está en `.gitignore` y NO se debe subir a Git porque contiene credenciales.

### Paso 2: Configurar Variables de Entorno

Copia el archivo `.env.example` y configura las credenciales:

```bash
cp .env.example .env
```

Edita el archivo `.env` y reemplaza los placeholders con tus credenciales reales:

```bash
# Ejemplo - NO uses estas credenciales en producción
POSTGRES_TIMESCALE_PASSWORD=tu_password_seguro
METADATA_PASSWORD=tu_password_seguro
REDIS_PASSWORD=tu_password_seguro
MQTT_USERNAME=tu_usuario_mqtt
MQTT_PASSWORD=tu_password_mqtt
EMQX_DASHBOARD_PASSWORD=tu_password_dashboard
```

**IMPORTANTE:** El archivo `.env` está en `.gitignore` y NUNCA se debe subir a Git.

### Paso 3: Levantar los servicios

```bash
# Levantar todos los servicios (API + bases de datos + Redis + EMQX)
docker-compose up -d

# Ver logs
docker-compose logs -f api

# Solo levantar la API (si las bases de datos ya están corriendo)
docker-compose up -d api
```

### Paso 3: Verificar que todo funciona

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Servicios disponibles en local:

| Servicio | Puerto | URL/Acceso |
|----------|--------|------------|
| API | 8080 | http://localhost:8080 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |
| TimescaleDB | 5432 | localhost:5432 |
| PostgreSQL Metadata | 5433 | localhost:5433 |
| Redis | 6379 | localhost:6379 |
| EMQX Dashboard | 18083 | http://localhost:18083 |
| EMQX MQTT | 1883 | tcp://localhost:1883 |

### Credenciales

**IMPORTANTE:** Las credenciales deben configurarse en el archivo `.env` (NO subir a Git).

Para desarrollo local, puedes usar credenciales de prueba. Para producción, usa credenciales fuertes y únicas.

Genera passwords seguros usando:
```bash
openssl rand -base64 32
```

### Detener los servicios:

```bash
# Detener
docker-compose down

# Detener y eliminar volúmenes (CUIDADO: borra todos los datos)
docker-compose down -v
```

---

## Despliegue en Kubernetes - Desarrollo

### Paso 1: Construir y publicar la imagen (automático con GitHub Actions)

Cuando haces push a la rama `develop`, GitHub Actions automáticamente:
1. Ejecuta los tests
2. Construye la imagen Docker
3. La publica en DockerHub con el tag `develop`

Para ver el progreso: https://github.com/apptolast/InvernaderosAPI/actions

### Paso 2: Desplegar en Kubernetes

```bash
# Desde el directorio raíz del proyecto
cd ../k8s/11-api-dev

# Ejecutar el script de despliegue
./deploy.sh
```

El script automáticamente:
- Crea el namespace `apptolast-invernadero-api-dev`
- Crea las bases de datos de desarrollo (`greenhouse_timeseries_dev`, `greenhouse_metadata_dev`)
- Aplica todos los manifests (secrets, configmaps, deployment, service, certificate, ingressroute)
- Espera a que el deployment esté listo
- Muestra el estado y las URLs de acceso

### Paso 3: Verificar el despliegue

```bash
# Ver todos los recursos
kubectl get all -n apptolast-invernadero-api-dev

# Ver logs de los pods
kubectl logs -n apptolast-invernadero-api-dev -l app=invernaderos-api -f

# Ver estado del certificado
kubectl get certificate -n apptolast-invernadero-api-dev

# Describir el deployment
kubectl describe deployment invernaderos-api -n apptolast-invernadero-api-dev
```

### URLs de acceso (Desarrollo):

- **API:** https://inverapi-dev.apptolast.com
- **Swagger UI:** https://inverapi-dev.apptolast.com/swagger-ui.html
- **API Docs:** https://inverapi-dev.apptolast.com/v3/api-docs
- **Health Check:** https://inverapi-dev.apptolast.com/actuator/health

### Actualizar el deployment (después de un nuevo push a develop):

```bash
# Forzar actualización de la imagen
kubectl rollout restart deployment/invernaderos-api -n apptolast-invernadero-api-dev

# Ver el progreso del rollout
kubectl rollout status deployment/invernaderos-api -n apptolast-invernadero-api-dev

# Ver historial de rollouts
kubectl rollout history deployment/invernaderos-api -n apptolast-invernadero-api-dev
```

---

## Despliegue en Kubernetes - Producción

### Paso 1: Merge a main

Cuando haces merge de `develop` a `main`, GitHub Actions automáticamente:
1. Ejecuta los tests
2. Construye la imagen Docker
3. La publica en DockerHub con los tags `latest` y `main`

### Paso 2: Desplegar en Kubernetes

```bash
# Desde el directorio raíz del proyecto
cd ../k8s/10-api-prod

# Ejecutar el script de despliegue
./deploy.sh
```

**IMPORTANTE:** El script pedirá confirmación antes de desplegar a producción.

### Paso 3: Verificar el despliegue

```bash
# Ver todos los recursos
kubectl get all -n apptolast-invernadero-api-prod

# Ver logs de los pods
kubectl logs -n apptolast-invernadero-api-prod -l app=invernaderos-api -f

# Ver estado del certificado
kubectl get certificate -n apptolast-invernadero-api-prod
```

### URLs de acceso (Producción):

- **API:** https://inverapi-prod.apptolast.com
- **Swagger UI:** https://inverapi-prod.apptolast.com/swagger-ui.html
- **API Docs:** https://inverapi-prod.apptolast.com/v3/api-docs
- **Health Check:** https://inverapi-prod.apptolast.com/actuator/health

### Rollback en producción:

```bash
# Ver historial de rollouts
kubectl rollout history deployment/invernaderos-api -n apptolast-invernadero-api-prod

# Hacer rollback a la versión anterior
kubectl rollout undo deployment/invernaderos-api -n apptolast-invernadero-api-prod

# Rollback a una versión específica
kubectl rollout undo deployment/invernaderos-api -n apptolast-invernadero-api-prod --to-revision=2
```

---

## CI/CD con GitHub Actions

### Workflow automático

El workflow `.github/workflows/build-and-push.yml` se ejecuta automáticamente cuando:
- Se hace push a `main` o `develop`
- Se crea un Pull Request hacia `main` o `develop`
- Se ejecuta manualmente desde la interfaz de GitHub

### Tags de imagen generados:

| Rama | Tags |
|------|------|
| `main` | `latest`, `main`, `sha-xxxxxxx` |
| `develop` | `develop`, `sha-xxxxxxx` |

### Secrets requeridos en GitHub:

- `DOCKERHUB_USERNAME`: Usuario de DockerHub (ya configurado)
- `DOCKERHUB_TOKEN`: Token de acceso de DockerHub (ya configurado)

### Ver el progreso:

1. Ve a https://github.com/apptolast/InvernaderosAPI/actions
2. Selecciona el workflow "Build and Push Docker Image"
3. Selecciona la ejecución específica para ver logs y detalles

---

## Verificación y Troubleshooting

### Health Checks

```bash
# Local
curl http://localhost:8080/actuator/health

# Desarrollo
curl https://inverapi-dev.apptolast.com/actuator/health

# Producción
curl https://inverapi-prod.apptolast.com/actuator/health
```

### Ver logs en Kubernetes

```bash
# Logs de un pod específico
kubectl logs -n <namespace> <pod-name>

# Logs de todos los pods de la API
kubectl logs -n <namespace> -l app=invernaderos-api --tail=100 -f

# Logs anteriores (si el pod reinició)
kubectl logs -n <namespace> <pod-name> --previous
```

### Problemas comunes

#### 1. El pod no arranca

```bash
# Verificar eventos
kubectl describe pod <pod-name> -n <namespace>

# Ver logs
kubectl logs <pod-name> -n <namespace>

# Verificar ConfigMap y Secrets
kubectl get configmap -n <namespace>
kubectl get secret -n <namespace>
```

#### 2. No se puede conectar a la base de datos

```bash
# Verificar conectividad desde el pod
kubectl exec -n <namespace> <pod-name> -- ping timescaledb.apptolast-invernadero-api.svc.cluster.local

# Verificar que las bases de datos existen
kubectl exec -n apptolast-invernadero-api statefulset/timescaledb -- psql -U admin -l
kubectl exec -n apptolast-invernadero-api statefulset/postgresql-metadata -- psql -U admin -l
```

#### 3. Certificado TLS no se genera

```bash
# Ver estado del certificado
kubectl describe certificate invernaderos-api-tls -n <namespace>

# Ver logs de cert-manager
kubectl logs -n cert-manager -l app=cert-manager -f

# Verificar ClusterIssuer
kubectl get clusterissuer
```

#### 4. IngressRoute no funciona

```bash
# Ver IngressRoute
kubectl describe ingressroute invernaderos-api -n <namespace>

# Ver logs de Traefik
kubectl logs -n kube-system -l app.kubernetes.io/name=traefik -f

# Verificar que el servicio existe
kubectl get svc -n <namespace>
```

### Comandos útiles

```bash
# Ver uso de recursos
kubectl top pods -n <namespace>

# Ejecutar shell en un pod
kubectl exec -it -n <namespace> <pod-name> -- /bin/sh

# Port-forward para acceso local
kubectl port-forward -n <namespace> svc/invernaderos-api 8080:8080

# Editar deployment en vivo (usar con cuidado)
kubectl edit deployment invernaderos-api -n <namespace>

# Escalar replicas manualmente
kubectl scale deployment invernaderos-api -n <namespace> --replicas=3
```

---

## Actualizaciones de Configuración

### Actualizar Secrets

```bash
# Editar secret
kubectl edit secret invernaderos-api-secret -n <namespace>

# O recrear desde archivo
kubectl delete secret invernaderos-api-secret -n <namespace>
kubectl apply -f <path-to-secret.yaml>

# Reiniciar pods para que tomen los nuevos valores
kubectl rollout restart deployment/invernaderos-api -n <namespace>
```

### Actualizar ConfigMap

```bash
# Editar ConfigMap
kubectl edit configmap invernaderos-api-config -n <namespace>

# O recrear desde archivo
kubectl delete configmap invernaderos-api-config -n <namespace>
kubectl apply -f <path-to-configmap.yaml>

# Reiniciar pods
kubectl rollout restart deployment/invernaderos-api -n <namespace>
```

---

## Monitoreo

### Actuator Endpoints

La API expone endpoints de Spring Boot Actuator para monitoreo:

- `/actuator/health` - Health check general
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Información de la aplicación
- `/actuator/metrics` - Métricas de Prometheus

### Prometheus

Las métricas están disponibles en formato Prometheus en `/actuator/prometheus` (si está habilitado).

---

## Recursos Adicionales

- [Documentación de Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Documentación de Kubernetes](https://kubernetes.io/docs/home/)
- [Documentación de Traefik](https://doc.traefik.io/traefik/)
- [Documentación de cert-manager](https://cert-manager.io/docs/)
- [Documentación de Docker Compose](https://docs.docker.com/compose/)

---

## Soporte

Para reportar problemas o solicitar ayuda:
- Crear un issue en: https://github.com/apptolast/InvernaderosAPI/issues
- Contactar al equipo de desarrollo

---

**Última actualización:** 2025-01-10
