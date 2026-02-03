# Guía de Migraciones con Flyway

> **Documentación basada en las best practices oficiales de Flyway**
> - [Recommended Practices](https://documentation.red-gate.com/fd/recommended-practices-150700352.html)
> - [Migrations Reference](https://documentation.red-gate.com/fd/migrations-271585107.html)
> - [CI/CD Integration](https://documentation.red-gate.com/fd/6-deploying-via-ci-cd-platforms-254153312.html)

---

## REGLAS DE ORO (NON-NEGOTIABLE)

Estas reglas son **ABSOLUTAS** y nunca deben violarse:

### 1. NUNCA modificar una migración ya aplicada

```
❌ PROHIBIDO:
   - Editar V20__add_column.sql después de que fue aplicada en cualquier entorno
   - Cambiar el contenido de una migración existente
   - "Arreglar" una migración que ya se ejecutó

✅ CORRECTO:
   - Crear una NUEVA migración V21__fix_column.sql
   - Siempre hacia adelante, nunca hacia atrás
```

**Por qué**: Flyway calcula un checksum (CRC32) del contenido del archivo. Si cambias el contenido, el checksum cambiará y Flyway fallará con "checksum mismatch".

### 2. NUNCA eliminar una migración ya aplicada

```
❌ PROHIBIDO:
   - Borrar V15__create_table.sql si ya fue aplicada
   - Mover archivos de migración a otra carpeta

✅ CORRECTO:
   - Mantener todos los archivos de migración en el repositorio
   - Si necesitas revertir cambios, crear una migración nueva
```

**Por qué**: Flyway espera encontrar todos los archivos de migraciones que están registrados en `flyway_schema_history`. Si falta un archivo, fallará con "missing migration".

### 3. Una migración = un cambio atómico

```
❌ PROHIBIDO:
   - V20__update_users_and_add_products_and_modify_orders.sql
   - Mezclar cambios no relacionados en una sola migración

✅ CORRECTO:
   - V20__add_email_column_to_users.sql
   - V21__create_products_table.sql
   - V22__add_status_column_to_orders.sql
```

**Por qué**: Si una migración falla a mitad de ejecución, es más fácil identificar y corregir el problema si cada migración hace una sola cosa.

### 4. Las migraciones son INMUTABLES

Una vez que haces commit de una migración, considérala **grabada en piedra**:

```
Commit: "Add V20__create_products_table.sql"
        ↓
Push to repository
        ↓
CI/CD aplica la migración a DEV
        ↓
=== PUNTO SIN RETORNO ===
        ↓
La migración V20 es ahora INMUTABLE
```

### 5. Siempre testear en DEV/Staging antes de PROD

```
Flujo correcto:
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    DEV      │ ──► │   STAGING   │ ──► │    PROD     │
│ (auto)      │     │ (auto)      │     │ (approval)  │
└─────────────┘     └─────────────┘     └─────────────┘
```

---

## Nomenclatura de Migraciones

### Formato estándar

```
V{version}__{description}.sql
│    │            │
│    │            └── Descripción en snake_case (separado por __)
│    └── Número de versión (puede ser entero o decimal)
└── Prefijo obligatorio para migraciones versionadas
```

### Ejemplos buenos y malos

```
✅ BUENOS:
   V20__add_email_to_users.sql
   V21__create_products_table.sql
   V22__add_index_on_orders_created_at.sql
   V23__drop_deprecated_legacy_table.sql

❌ MALOS:
   V20_add_email.sql              # Solo un underscore (necesita __)
   V20__AddEmail.sql              # CamelCase en lugar de snake_case
   20__add_email.sql              # Falta el prefijo V
   V20__add email to users.sql    # Espacios en el nombre
```

### Versionado recomendado

Para proyectos que crecen:

```
V001__initial_schema.sql
V002__add_users_table.sql
V003__add_products_table.sql
...
V100__major_refactoring.sql
```

---

## Estrategia por Entorno

### DEV (development)

```yaml
# application-dev.yaml
flyway:
  auto-migrate: true  # Migra automáticamente al arrancar
```

- Migraciones se aplican automáticamente
- Permite iteración rápida
- Si algo falla, es fácil recrear la BD

### PROD (production)

```yaml
# application-prod.yaml
flyway:
  auto-migrate: false  # Solo valida, NO migra
```

- Migraciones via CI/CD con aprobación manual
- La app solo VALIDA que el schema es correcto
- Rollback del pod NO afecta la base de datos

---

## Cómo Crear una Nueva Migración

### Paso 1: Determinar el siguiente número de versión

```bash
# Ver la última versión en el directorio
ls -la src/main/resources/db/migration/ | tail -5
# V28__change_greenhouse_to_sector.sql
# V29__add_description_fields.sql
# V30__rename_sectors_variety.sql

# La siguiente versión es V31
```

### Paso 2: Crear el archivo

```bash
# Crear archivo con nombre descriptivo
touch src/main/resources/db/migration/V31__add_phone_to_users.sql
```

### Paso 3: Escribir el SQL

```sql
-- V31: Add phone column to users table
-- Fecha: 2026-02-03
-- Autor: Pablo

-- Añadir columna phone
ALTER TABLE metadata.users
ADD COLUMN phone VARCHAR(20);

-- Crear índice si es necesario
CREATE INDEX idx_users_phone ON metadata.users(phone);
```

### Paso 4: Testear localmente

```bash
# Opción 1: Ejecutar la app en modo DEV
./gradlew bootRun

# Opción 2: Usar Flyway CLI
flyway -url=jdbc:postgresql://localhost:5433/greenhouse_metadata_dev \
       -user=admin -password=xxx \
       -schemas=metadata \
       -locations=filesystem:src/main/resources/db/migration \
       migrate
```

### Paso 5: Commit y push

```bash
git add src/main/resources/db/migration/V31__add_phone_to_users.sql
git commit -m "Add V31 migration: add phone column to users"
git push origin develop
```

---

## Cómo Manejar Errores

### Error: Checksum mismatch

```
ERROR: Migration checksum mismatch for migration version 20
-> Applied checksum : 1234567890
-> Resolved checksum: 9876543210
```

**Causa**: El archivo V20 fue modificado después de aplicarse.

**Soluciones**:

1. **Si el cambio fue intencional** (ej: fix de typo que no afecta el schema):
   ```bash
   # Ejecutar repair para actualizar el checksum
   flyway repair
   ```

2. **Si el cambio fue accidental**:
   ```bash
   # Revertir el archivo a su versión original
   git checkout origin/main -- src/main/resources/db/migration/V20__xxx.sql
   ```

### Error: Missing migration

```
ERROR: Detected resolved migration not applied to database: 15
```

**Causa**: El archivo V15 fue eliminado pero la BD lo tiene registrado.

**Soluciones**:

1. **Restaurar el archivo** (recomendado):
   ```bash
   git checkout origin/main -- src/main/resources/db/migration/V15__xxx.sql
   ```

2. **Marcar como deleted** (si el archivo realmente no existe):
   ```bash
   flyway repair
   ```

### Error: Failed migration

```
ERROR: Migration V20 failed
...
Caused by: org.postgresql.util.PSQLException: ERROR: column "xxx" already exists
```

**Soluciones**:

1. **Arreglar manualmente la BD** (si es posible):
   ```sql
   -- Limpiar el estado incompleto
   DROP COLUMN IF EXISTS xxx;
   ```

2. **Marcar la migración como resuelta**:
   ```sql
   -- Actualizar flyway_schema_history
   UPDATE metadata.flyway_schema_history
   SET success = true
   WHERE version = '20';
   ```

3. **Ejecutar repair**:
   ```bash
   flyway repair
   ```

---

## Patrones de Migración Seguros

### Añadir columna nullable (SEGURO)

```sql
-- ✅ SEGURO: No requiere lock largo, no afecta datos existentes
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

### Añadir columna NOT NULL (REQUIERE CUIDADO)

```sql
-- ✅ CORRECTO: Añadir con default primero
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';

-- ❌ PELIGROSO: Fallará si hay datos existentes
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL;
```

### Renombrar columna (PELIGROSO)

```sql
-- ❌ PELIGROSO: La app puede fallar durante el deploy
ALTER TABLE users RENAME COLUMN name TO full_name;

-- ✅ SEGURO: Migración en fases
-- V31: Añadir nueva columna
ALTER TABLE users ADD COLUMN full_name VARCHAR(200);
-- V32: Copiar datos (puede ser script separado)
UPDATE users SET full_name = name WHERE full_name IS NULL;
-- V33: En un deploy posterior, eliminar la columna vieja
-- ALTER TABLE users DROP COLUMN name;
```

### Eliminar columna (MUY PELIGROSO)

```sql
-- ❌ MUY PELIGROSO: Si la app aún usa la columna, fallará
ALTER TABLE users DROP COLUMN deprecated_field;

-- ✅ SEGURO: Proceso en fases
-- 1. Primero: Eliminar uso de la columna en el código
-- 2. Deploy nuevo código que no usa la columna
-- 3. Esperar a que todos los pods estén actualizados
-- 4. Solo entonces: CREATE MIGRATION para DROP COLUMN
```

---

## Checklist para Nuevas Migraciones

Antes de hacer commit de una migración, verifica:

- [ ] El número de versión es correcto (siguiente en secuencia)
- [ ] El nombre describe claramente qué hace
- [ ] El SQL es idempotente si es posible (IF EXISTS, IF NOT EXISTS)
- [ ] Probado localmente en DEV
- [ ] No modifica ninguna migración existente
- [ ] No elimina ninguna migración existente
- [ ] Los cambios son compatibles hacia atrás con la versión actual de la app

---

## Comandos Útiles

### Ver estado actual

```bash
flyway info
```

### Validar sin ejecutar

```bash
flyway validate
```

### Ejecutar migraciones

```bash
flyway migrate
```

### Reparar tabla de historial

```bash
flyway repair
```

### Ver historial en la BD

```sql
SELECT version, description, success, installed_on, execution_time
FROM metadata.flyway_schema_history
ORDER BY installed_rank;
```

---

## Referencias

- [Flyway Official Documentation](https://documentation.red-gate.com/flyway)
- [Recommended Practices](https://documentation.red-gate.com/fd/recommended-practices-150700352.html)
- [Repair Command](https://documentation.red-gate.com/fd/repair-277578892.html)
- [CI/CD Integration](https://documentation.red-gate.com/fd/6-deploying-via-ci-cd-platforms-254153312.html)
- [Spring Boot + Flyway](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
