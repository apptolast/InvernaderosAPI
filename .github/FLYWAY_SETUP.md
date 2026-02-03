# Configuración de Flyway CI/CD

Este documento describe cómo configurar los workflows de GitHub Actions para las migraciones de base de datos con Flyway.

## Secrets Necesarios

Debes configurar los siguientes secrets en tu repositorio de GitHub:

### Para ambiente DEV

Ve a: `Settings` → `Secrets and variables` → `Actions` → `New repository secret`

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `FLYWAY_DB_URL` | URL JDBC de la base de datos DEV | `jdbc:postgresql://138.199.157.58:30433/greenhouse_metadata_dev` |
| `FLYWAY_DB_USER` | Usuario de la base de datos | `admin` |
| `FLYWAY_DB_PASSWORD` | Password de la base de datos | `tu_password` |

### Para ambiente PROD

Los mismos secrets pero para PROD:

| Secret Name | Descripción | Ejemplo |
|-------------|-------------|---------|
| `FLYWAY_DB_URL` | URL JDBC de la base de datos PROD | `jdbc:postgresql://138.199.157.58:30433/greenhouse_metadata` |
| `FLYWAY_DB_USER` | Usuario de la base de datos | `admin` |
| `FLYWAY_DB_PASSWORD` | Password de la base de datos | `tu_password` |

## Configurar Environments en GitHub

Para que PROD requiera aprobación manual antes de ejecutar migraciones:

1. Ve a `Settings` → `Environments`
2. Crea dos environments: `dev` y `prod`
3. Para `prod`:
   - Marca "Required reviewers"
   - Añade los reviewers que deben aprobar las migraciones
   - Opcionalmente, marca "Wait timer" (ej: 5 minutos de espera)

## Flujo de Trabajo

```
┌─────────────────────────────────────────────────────────────────┐
│                         DEVELOP BRANCH                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Developer crea migración V32__add_new_column.sql           │
│  2. Developer hace commit y push a develop                      │
│  3. GitHub Actions detecta cambios en db/migration/            │
│  4. Workflow ejecuta:                                           │
│     a) flyway info (muestra estado actual)                     │
│     b) flyway validate (verifica checksums)                    │
│     c) flyway migrate (aplica migraciones a DEV)               │
│  5. Si éxito → build-and-push.yml construye imagen :develop    │
│  6. Image Updater actualiza pod DEV                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                          MAIN BRANCH                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. PR de develop → main (ya probado en DEV)                   │
│  2. Merge a main                                                │
│  3. GitHub Actions detecta cambios                             │
│  4. Workflow ejecuta:                                           │
│     a) flyway info                                              │
│     b) flyway validate                                          │
│     c) ⏸️ PAUSA: Espera aprobación de reviewer                 │
│     d) flyway migrate (aplica migraciones a PROD)              │
│  5. Si éxito → build-and-push.yml construye imagen :latest     │
│  6. Image Updater actualiza pod PROD                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Ejecución Manual

El workflow puede ejecutarse manualmente desde la pestaña "Actions" → "Flyway Database Migrations" → "Run workflow".

Opciones disponibles:
- **environment**: `dev` o `prod`
- **action**:
  - `info`: Solo muestra el estado actual
  - `validate`: Verifica que las migraciones son válidas
  - `migrate`: Ejecuta las migraciones pendientes
  - `repair`: Repara la tabla flyway_schema_history (usar con cuidado!)
- **dry_run**: Si es `true`, solo muestra qué haría sin ejecutar cambios

### Cuándo usar cada action:

| Situación | Action | Dry Run |
|-----------|--------|---------|
| Ver estado actual | `info` | N/A |
| Verificar antes de deploy | `validate` | N/A |
| Aplicar migraciones | `migrate` | `false` |
| Ver qué migraciones se aplicarían | `migrate` | `true` |
| Checksum mismatch | `repair` | `false` |

## Troubleshooting

### Error: Checksum mismatch

```
ERROR: Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version X
```

**Causa**: Alguien modificó un archivo de migración ya aplicado.

**Solución**:
1. Ejecutar workflow manual con `action=repair`
2. O ejecutar en la BD: `SELECT * FROM metadata.flyway_schema_history WHERE version = 'X';`

### Error: Migration missing

```
ERROR: Detected resolved migration not applied to database: X
```

**Causa**: Un archivo de migración fue eliminado.

**Solución**:
1. Restaurar el archivo de migración, O
2. Ejecutar `repair` para marcar como deleted

### Error: Out of order migration

```
ERROR: Detected resolved migration not applied to database: X
Applied migration: Y > Pending: X
```

**Causa**: Una migración con versión anterior a las ya aplicadas.

**Solución**:
1. Renumerar la migración con versión mayor
2. O configurar `outOfOrder=true` (no recomendado)

## Referencias

- [Flyway Recommended Practices](https://documentation.red-gate.com/fd/recommended-practices-150700352.html)
- [Flyway Repair Command](https://documentation.red-gate.com/fd/repair-277578892.html)
- [Flyway CI/CD Integration](https://documentation.red-gate.com/fd/6-deploying-via-ci-cd-platforms-254153312.html)
