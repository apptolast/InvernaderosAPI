# Security Guide - InvernaderosAPI

## 🔒 Overview

This document outlines security best practices and guidelines for the InvernaderosAPI project.

## ⚠️ NEVER Commit Credentials

**CRITICAL:** Never commit any of the following to version control:

- ❌ Passwords
- ❌ API keys
- ❌ Access tokens
- ❌ Private SSH keys
- ❌ Database credentials
- ❌ MQTT credentials
- ❌ Redis passwords
- ❌ Any other secrets

## 📁 Protected Files

The following files are protected by `.gitignore` and should NEVER be committed:

- `.env`
- `.env.local`
- `.env.*.local`
- `application-local.yaml`
- `docker-compose.override.yaml`
- `*.key`
- `*.pem`
- `*.p12`
- `*.jks`
- `secrets/`
- `credentials/`

## 🔐 Environment Variables Setup

### Local Development

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` and set your local credentials:**
   ```bash
   # Generate secure passwords
   openssl rand -base64 32
   ```

3. **Set all required variables** in the `.env` file (see `.env.example` for template)

### Production/Staging

**NEVER use plaintext environment variables in production!**

Use a secure secret management solution:

#### Option 1: Kubernetes Secrets

Example Kubernetes secret configuration:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: invernaderos-api-secret
  namespace: apptolast-invernadero-api-prod
type: Opaque
stringData:
  TIMESCALE_PASSWORD: "your-secure-password"
  METADATA_PASSWORD: "your-secure-password"
  REDIS_PASSWORD: "your-secure-password"
  MQTT_USERNAME: "your-username"
  MQTT_PASSWORD: "your-secure-password"
```

Apply the secret:
```bash
kubectl apply -f secret.yaml
```

**IMPORTANT:** Do not commit `secret.yaml` to the repository. Manage it separately or use tools like Sealed Secrets.

---

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
type: Opaque
data:
  postgres-password: <base64-encoded-value>
  redis-password: <base64-encoded-value>
  mqtt-password: <base64-encoded-value>
```

#### Option 2: AWS Secrets Manager
```bash
aws secretsmanager create-secret \
  --name invernaderos-api/database \
  --secret-string '{"password":"your-secure-password"}'
```

#### Option 3: HashiCorp Vault
```bash
vault kv put secret/invernaderos-api/database \
  password="your-secure-password"
```

#### Option 4: Azure Key Vault
```bash
az keyvault secret set \
  --vault-name invernaderos-vault \
  --name database-password \
  --value "your-secure-password"
```

## 🛡️ Security Best Practices

### Password Requirements

For production environments, use passwords that meet these criteria:

- ✅ Minimum 16 characters
- ✅ Mix of uppercase, lowercase, numbers, and special characters
- ✅ Generated using cryptographically secure random generator
- ✅ Unique for each service/environment
- ✅ Rotated regularly (every 90 days recommended)

### Generate Secure Passwords

```bash
# Generate a 32-character base64 password
openssl rand -base64 32

# Generate a 24-character alphanumeric password
openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 24

# Generate using pwgen (install first: apt-get install pwgen)
pwgen -s 32 1
```

### Credential Rotation

1. **Schedule:** Rotate credentials every 90 days minimum
2. **Process:**
   - Generate new credentials
   - Update in secret manager
   - Update application configuration
   - Restart services
   - Revoke old credentials
   - Monitor for any failures

### Database Security

1. **Use separate credentials for each environment**
   - Development: `greenhouse_timeseries_dev`, `greenhouse_metadata_dev`
   - Production: `greenhouse_timeseries`, `greenhouse_metadata`

2. **Principle of Least Privilege**
   - Grant only necessary permissions
   - Use read-only users for analytics
   - Create separate users for migrations

3. **Connection Security**
   - Use SSL/TLS for database connections in production
   - Configure `spring.datasource.hikari.connection-test-query`
   - Set appropriate timeout values

### MQTT Security

1. **Authentication:**
   - Use unique credentials per client
   - Implement ACL (Access Control Lists)
   - Use client certificates in production

2. **Connection Security:**
   - Use TLS/SSL (port 8883) instead of plain TCP (port 1883)
   - Use WSS (WebSocket Secure) for browser clients
   - Configure proper cipher suites

3. **EMQX Configuration:**
   ```yaml
   # In production, enable authentication plugin
   authentication:
     - mechanism: password_based
       backend: postgresql
   
   # Enable ACL
   authorization:
     sources:
       - type: postgresql
   ```

### Redis Security

1. **Authentication:**
   - Always set a strong password
   - Use ACL for fine-grained access control (Redis 6+)

2. **Network Security:**
   - Bind to localhost or private network only
   - Use TLS for connections
   - Configure firewall rules

3. **Configuration:**
   ```bash
   # redis.conf
   requirepass <strong-password>
   bind 127.0.0.1 ::1
   protected-mode yes
   ```

## 🔍 Security Scanning

### Before Committing

1. **Scan for secrets:**
   ```bash
   # Install git-secrets
   git secrets --scan
   
   # Install truffleHog
   trufflehog git file://. --only-verified
   ```

2. **Check for exposed credentials:**
   ```bash
   grep -r "password\|secret\|key\|token" . --exclude-dir=.git --exclude-dir=node_modules
   ```

### Automated Scanning

The project uses:
- **CodeQL** - Automated security scanning in CI/CD
- **Dependabot** - Dependency vulnerability scanning
- **GitHub Secret Scanning** - Detects committed secrets

## 🚨 If Credentials Are Exposed

If you accidentally commit credentials:

1. **Immediately rotate the exposed credentials**
2. **Remove from git history:**
   ```bash
   # Use BFG Repo-Cleaner or git-filter-branch
   bfg --replace-text passwords.txt
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   ```
3. **Force push to remote** (coordinate with team):
   ```bash
   git push --force --all
   ```
4. **Notify the team**
5. **Check logs for unauthorized access**
6. **Document the incident**

## 📋 Security Checklist

Before deploying:

- [ ] All credentials are stored in secret managers
- [ ] No plaintext passwords in code or configuration
- [ ] `.env` file is in `.gitignore`
- [ ] Strong passwords are used (16+ characters)
- [ ] Credentials are unique per environment
- [ ] SSL/TLS is enabled for all connections
- [ ] Firewall rules are properly configured
- [ ] Security scanning is enabled
- [ ] Logs don't contain sensitive information
- [ ] Monitoring and alerting are configured

## 🔗 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Best Practices](https://docs.spring.io/spring-security/reference/features/index.html)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/security.html)
- [Redis Security](https://redis.io/docs/management/security/)
- [EMQX Security](https://www.emqx.io/docs/en/latest/security/security.html)

## 📞 Reporting Security Issues

If you discover a security vulnerability:

1. **DO NOT** create a public GitHub issue
2. Report via [GitHub Security Advisories](https://github.com/apptolast/InvernaderosAPI/security/advisories/new)
3. Alternatively, email the security team at: security@apptolast.com
4. Provide detailed information about the vulnerability
5. Allow time for the issue to be fixed before public disclosure

---

**Last Updated:** 2024-11-11
**Version:** 1.0.0

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
