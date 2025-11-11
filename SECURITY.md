# 🔒 Guía de Seguridad - Gestión de Credenciales

## 📋 Resumen

Esta guía explica cómo gestionar de forma segura las credenciales y secretos en el proyecto Invernaderos API.

## ⚠️ Principios de Seguridad

### NUNCA hagas esto:
- ❌ Hardcodear credenciales en código fuente
- ❌ Subir archivos `.env` al repositorio
- ❌ Compartir contraseñas en issues, pull requests o comentarios
- ❌ Usar contraseñas débiles o por defecto
- ❌ Reutilizar contraseñas entre entornos
- ❌ Documentar credenciales reales en archivos README o DEPLOYMENT

### SIEMPRE haz esto:
- ✅ Usar variables de entorno para credenciales
- ✅ Mantener archivos `.env` en `.gitignore`
- ✅ Usar contraseñas seguras y únicas
- ✅ Rotar credenciales regularmente
- ✅ Usar gestores de secretos en producción
- ✅ Documentar el formato requerido, no los valores reales

## 🔐 Configuración de Credenciales

### Desarrollo Local

1. **Copiar archivos de ejemplo:**
   ```bash
   cp .env.example .env
   cp docker-compose.override.yaml.example docker-compose.override.yaml
   cp application-local.yaml.example application-local.yaml
   ```

2. **Editar `.env` con tus credenciales:**
   ```bash
   # Edita el archivo y establece contraseñas seguras
   nano .env  # o tu editor preferido
   ```

3. **Generar contraseñas seguras:**
   ```bash
   # Opción 1: usar openssl
   openssl rand -base64 32
   
   # Opción 2: usar pwgen
   pwgen -s 32 1
   
   # Opción 3: usar Python
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

4. **Verificar que `.env` está en .gitignore:**
   ```bash
   git status  # .env NO debe aparecer en la lista
   ```

### Entornos de Producción

#### Kubernetes Secrets

Usar Kubernetes Secrets para gestionar credenciales:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: invernaderos-api-secret
  namespace: apptolast-invernadero-api-prod
type: Opaque
stringData:
  TIMESCALE_PASSWORD: "tu-password-seguro"
  METADATA_PASSWORD: "tu-password-seguro"
  REDIS_PASSWORD: "tu-password-seguro"
  MQTT_USERNAME: "tu-username"
  MQTT_PASSWORD: "tu-password-seguro"
```

Crear el secret:
```bash
kubectl apply -f secret.yaml
```

**IMPORTANTE:** No subir `secret.yaml` al repositorio. Gestionarlo por separado o usar herramientas como Sealed Secrets.

#### AWS Secrets Manager

Para aplicaciones en AWS:

```bash
# Crear secret
aws secretsmanager create-secret \
  --name invernaderos/production/database \
  --secret-string '{"password":"tu-password-seguro"}'

# Leer secret en la aplicación
aws secretsmanager get-secret-value \
  --secret-id invernaderos/production/database
```

#### Azure Key Vault

Para aplicaciones en Azure:

```bash
# Crear secret
az keyvault secret set \
  --vault-name invernaderos-keyvault \
  --name database-password \
  --value "tu-password-seguro"

# Leer secret
az keyvault secret show \
  --vault-name invernaderos-keyvault \
  --name database-password
```

## 📁 Archivos Sensibles

### Archivos que DEBEN estar en .gitignore:

```
.env
.env.local
.env.*.local
application-local.yaml
docker-compose.override.yaml
*.key
*.pem
*.p12
*.jks
secrets/
credentials/
```

### Archivos que SÍ se pueden subir:

```
.env.example
application-local.yaml.example
docker-compose.override.yaml.example
```

Estos archivos contienen la estructura pero SIN valores reales.

## 🔄 Rotación de Credenciales

### Cuándo rotar credenciales:

1. **Inmediatamente:**
   - Si se exponen en el repositorio
   - Si se detecta un acceso no autorizado
   - Si un empleado deja el equipo

2. **Periódicamente:**
   - Cada 90 días para producción
   - Cada 180 días para desarrollo

### Proceso de rotación:

1. Generar nuevas credenciales
2. Actualizar en el gestor de secretos
3. Reiniciar servicios que usan las credenciales
4. Verificar que todo funciona
5. Revocar credenciales antiguas
6. Documentar el cambio (sin incluir las credenciales)

## 🛡️ Buenas Prácticas por Servicio

### PostgreSQL / TimescaleDB

```bash
# Contraseña segura (mínimo 16 caracteres)
TIMESCALE_PASSWORD=$(openssl rand -base64 24)
METADATA_PASSWORD=$(openssl rand -base64 24)

# Diferentes contraseñas por entorno
# DEV:  TIMESCALE_PASSWORD=dev_password_xxx
# PROD: TIMESCALE_PASSWORD=prod_password_yyy
```

### Redis

```bash
# Contraseña segura
REDIS_PASSWORD=$(openssl rand -base64 24)

# Configurar ACL para mayor seguridad
# redis-cli ACL SETUSER api_user on >password ~* +@all
```

### MQTT / EMQX

```bash
# Usuario y contraseña específicos
MQTT_USERNAME="api_user_$(date +%s)"
MQTT_PASSWORD=$(openssl rand -base64 24)

# Usar diferentes usuarios por servicio/componente
```

## 🔍 Detección de Credenciales Expuestas

### Herramientas recomendadas:

1. **git-secrets** - Previene commits con secretos
   ```bash
   git secrets --install
   git secrets --register-aws
   ```

2. **truffleHog** - Busca secretos en el historial
   ```bash
   trufflehog git https://github.com/apptolast/InvernaderosAPI
   ```

3. **gitleaks** - Escaneo rápido de secretos
   ```bash
   gitleaks detect --source . --verbose
   ```

4. **GitHub Secret Scanning** - Automático en repositorios públicos

### Validación local antes de commit:

```bash
# Crear un pre-commit hook básico
# Nota: Este es un ejemplo simple. Para mayor seguridad, use herramientas como git-secrets o gitleaks.
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash

# Obtiene la lista de archivos staged, excluyendo archivos .example
files=$(git diff --cached --name-only | grep -v '\.example$')

# Busca palabras clave en los archivos filtrados
if [ -n "$files" ]; then
  if echo "$files" | xargs grep -IinE "password|secret|key|token" 2>/dev/null | grep -v "^[[:space:]]*#"; then
    echo "⚠️  Posible credencial detectada en commit"
    echo "Verifica que no estés subiendo información sensible"
    exit 1
  fi
fi
EOF

chmod +x .git/hooks/pre-commit
```

**Nota:** Este hook es solo un ejemplo básico que puede generar falsos positivos. Para una solución más robusta, considere usar herramientas especializadas como `git-secrets`, `truffleHog` o `gitleaks` que están diseñadas específicamente para detectar credenciales.

## 📞 Qué hacer si se exponen credenciales

### Plan de Respuesta de Incidentes:

1. **Inmediato (< 1 hora):**
   - ✅ Revocar/cambiar todas las credenciales expuestas
   - ✅ Verificar accesos no autorizados
   - ✅ Notificar al equipo

2. **Corto plazo (< 24 horas):**
   - ✅ Eliminar credenciales del código
   - ✅ Hacer commit y push de los cambios
   - ✅ Revisar logs de acceso
   - ✅ Documentar el incidente

3. **Mediano plazo (< 1 semana):**
   - ✅ Limpiar historial de git (si es necesario)
   - ✅ Implementar controles adicionales
   - ✅ Actualizar documentación de seguridad
   - ✅ Capacitar al equipo

### Limpieza de historial de Git:

```bash
# CUIDADO: Esto reescribe el historial
# Solo usar en coordinación con el equipo

# Usar BFG Repo-Cleaner
java -jar bfg.jar --replace-text passwords.txt

# O usar git filter-branch (más manual)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch path/to/secret/file" \
  --prune-empty --tag-name-filter cat -- --all
```

## ✅ Checklist de Seguridad

Antes de hacer commit:

- [ ] No hay contraseñas hardcodeadas en el código
- [ ] Archivo `.env` está en `.gitignore` y no se sube
- [ ] Variables de entorno usadas en lugar de valores literales
- [ ] Archivos `.example` no contienen credenciales reales
- [ ] Documentación no expone credenciales
- [ ] Pre-commit hooks instalados

Antes de desplegar:

- [ ] Credenciales rotadas desde último despliegue (si aplica)
- [ ] Contraseñas únicas por entorno
- [ ] Gestor de secretos configurado (producción)
- [ ] Logs no exponen credenciales
- [ ] Conexiones usan TLS/SSL
- [ ] Principio de menor privilegio aplicado

## 📚 Referencias

- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [12 Factor App - Config](https://12factor.net/config)
- [GitHub Security Best Practices](https://docs.github.com/en/code-security/getting-started/best-practices-for-preventing-data-leaks-in-your-organization)

## 🆘 Soporte

Si tienes dudas sobre gestión de credenciales:
1. Consulta esta guía
2. Revisa los archivos `.example`
3. Pregunta en el canal de seguridad del equipo
4. NO expongas credenciales reales al pedir ayuda

---

**Última actualización:** 2025-11-11  
**Revisión:** v1.0
