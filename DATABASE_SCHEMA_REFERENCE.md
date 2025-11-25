# DATABASE SCHEMA REFERENCE - InvernaderosAPI

**Fecha:** 2025-11-17
**Propósito:** Referencia COMPLETA de esquemas para evitar errores de Hibernate

---

## PROBLEMAS IDENTIFICADOS

### ❌ Error Principal: PostgreSQL ARRAY Types

**Hibernate schema validation falla con:**
```
Schema-validation: wrong column type encountered in column [actions_taken] in table [metadata.alert_resolution_history];
found [_text (Types#ARRAY)], but expecting [text (Types#VARCHAR)]
```

**Causa:**
- Base de datos: `text[]` (PostgreSQL ARRAY)
- Entidad JPA: `Array<String>` con `columnDefinition = "text[]"`
- Hibernate interpreta esto como VARCHAR en lugar de ARRAY

**Solución:**
- NO usar `Array<String>`
- Usar `@Type(value = io.hypersistence.utils.hibernate.type.array.StringArrayType::class)`
- O simplemente NO mapear el campo (readonly entities)

---

## TABLAS CON POSTGRESQL ARRAYS

### 1. metadata.alert_resolution_history
```sql
actions_taken | text[] | NULL | Array de acciones tomadas para resolver la alerta
```

**Tipo correcto en Kotlin:**
```kotlin
@Column(name = "actions_taken")
@JdbcTypeCode(SqlTypes.ARRAY)
@Type(io.hypersistence.utils.hibernate.type.array.ListArrayType::class)
val actionsTaken: List<String>? = null
```

### 2. metadata.audit_log
```sql
changed_fields | text[] | NULL | Array de nombres de campos que cambiaron
```

**Tipo correcto en Kotlin:**
```kotlin
@Column(name = "changed_fields")
@JdbcTypeCode(SqlTypes.ARRAY)
@Type(io.hypersistence.utils.hibernate.type.array.ListArrayType::class)
val changedFields: List<String>? = null
```

---

## ESQUEMA COMPLETO - metadata.alert_resolution_history

```
Column                | Type                     | Nullable
----------------------|--------------------------|----------
id                    | bigint                   | NOT NULL (PK, auto)
alert_id              | uuid                     | NOT NULL (FK → alerts)
previous_status       | varchar(20)              | NULL
previous_severity_id  | smallint                 | NULL (FK → alert_severities)
resolved_by           | uuid                     | NOT NULL (FK → users)
resolved_at           | timestamptz              | NOT NULL (default: now())
resolution_action     | varchar(50)              | NULL
resolution_notes      | text                     | NULL
actions_taken         | text[]                   | NULL  ⚠️ ARRAY TYPE
time_to_resolution    | interval                 | NULL
created_at            | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.audit_log

```
Column               | Type                     | Nullable
---------------------|--------------------------|----------
id                   | bigint                   | NOT NULL (PK, auto)
table_name           | varchar(100)             | NOT NULL
record_id            | uuid                     | NOT NULL
operation            | varchar(10)              | NOT NULL (CHECK: INSERT/UPDATE/DELETE)
old_values           | jsonb                    | NULL
new_values           | jsonb                    | NULL
changed_fields       | text[]                   | NULL  ⚠️ ARRAY TYPE
changed_by           | uuid                     | NULL (FK → users)
changed_by_username  | varchar(100)             | NULL
changed_at           | timestamptz              | NOT NULL (default: now())
change_reason        | text                     | NULL
ip_address           | inet                     | NULL
user_agent           | text                     | NULL
session_id           | varchar(100)             | NULL
application_version  | varchar(50)              | NULL
created_at           | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.actuator_command_history

```
Column                | Type                     | Nullable
----------------------|--------------------------|----------
id                    | bigint                   | NOT NULL (PK, auto)
actuator_id           | uuid                     | NOT NULL (FK → actuators)
greenhouse_id         | uuid                     | NOT NULL (FK → greenhouses)
tenant_id             | uuid                     | NOT NULL (FK → tenants)
command               | varchar(50)              | NOT NULL
target_value          | double precision         | NULL
target_state_id       | smallint                 | NULL (FK → actuator_states)
previous_state_id     | smallint                 | NULL (FK → actuator_states)
previous_value        | double precision         | NULL
new_state_id          | smallint                 | NULL (FK → actuator_states)
new_value             | double precision         | NULL
triggered_by          | varchar(20)              | NOT NULL (CHECK: USER/AUTOMATION/SCHEDULE/ALERT/API/SYSTEM)
triggered_by_user_id  | uuid                     | NULL (FK → users)
triggered_by_rule_id  | uuid                     | NULL
command_sent_at       | timestamptz              | NOT NULL (default: now())
command_executed_at   | timestamptz              | NULL
execution_status      | varchar(20)              | NULL (default: PENDING)
error_message         | text                     | NULL
metadata              | jsonb                    | NULL
created_at            | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.sensor_configuration_history

```
Column                | Type                     | Nullable
----------------------|--------------------------|----------
id                    | bigint                   | NOT NULL (PK, auto)
sensor_id             | uuid                     | NOT NULL (FK → sensors)
old_sensor_type_id    | smallint                 | NULL
old_unit_id           | smallint                 | NULL
old_min_threshold     | numeric(10,2)            | NULL
old_max_threshold     | numeric(10,2)            | NULL
old_calibration_data  | jsonb                    | NULL
old_mqtt_field_name   | varchar(100)             | NULL
new_sensor_type_id    | smallint                 | NULL
new_unit_id           | smallint                 | NULL
new_min_threshold     | numeric(10,2)            | NULL
new_max_threshold     | numeric(10,2)            | NULL
new_calibration_data  | jsonb                    | NULL
new_mqtt_field_name   | varchar(100)             | NULL
changed_by            | uuid                     | NULL (FK → users)
changed_at            | timestamptz              | NOT NULL (default: now())
change_reason         | text                     | NULL
change_type           | varchar(50)              | NULL
created_at            | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.bulk_operation_log

```
Column              | Type                     | Nullable
--------------------|--------------------------|----------
id                  | bigint                   | NOT NULL (PK, auto)
operation_type      | varchar(50)              | NOT NULL
target_table        | varchar(100)             | NOT NULL
tenant_id           | uuid                     | NULL (FK → tenants)
started_at          | timestamptz              | NOT NULL (default: now())
completed_at        | timestamptz              | NULL
status              | varchar(20)              | NULL (default: RUNNING)
total_records       | integer                  | NOT NULL (default: 0)
successful_records  | integer                  | NOT NULL (default: 0)
failed_records      | integer                  | NOT NULL (default: 0)
skipped_records     | integer                  | NOT NULL (default: 0)
error_summary       | jsonb                    | NULL
error_details       | text                     | NULL
duration_seconds    | integer                  | NULL
records_per_second  | numeric(10,2)            | NULL
triggered_by        | uuid                     | NULL (FK → users)
source_file         | varchar(255)             | NULL
metadata            | jsonb                    | NULL
created_at          | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.data_quality_log

```
Column                  | Type                     | Nullable
------------------------|--------------------------|----------
id                      | bigint                   | NOT NULL (PK, auto)
data_source             | varchar(50)              | NOT NULL
quality_issue_type      | varchar(50)              | NOT NULL
severity                | varchar(20)              | NULL (CHECK: LOW/MEDIUM/HIGH/CRITICAL)
greenhouse_id           | uuid                     | NULL
sensor_id               | uuid                     | NULL
tenant_id               | uuid                     | NULL
detected_at             | timestamptz              | NOT NULL (default: now())
time_range_start        | timestamptz              | NULL
time_range_end          | timestamptz              | NULL
affected_records_count  | integer                  | NULL
description             | text                     | NOT NULL
sample_data             | jsonb                    | NULL
status                  | varchar(20)              | NULL (default: OPEN)
resolved_at             | timestamptz              | NULL
resolved_by             | uuid                     | NULL
resolution_notes        | text                     | NULL
created_at              | timestamptz              | NULL (default: now())
```

---

## ESQUEMA COMPLETO - metadata.greenhouse_snapshot

```
Column                      | Type                     | Nullable
----------------------------|--------------------------|----------
id                          | uuid                     | NOT NULL (PK)
greenhouse_id               | uuid                     | NOT NULL (FK → greenhouses)
tenant_id                   | uuid                     | NOT NULL (FK → tenants)
snapshot_time               | timestamptz              | NOT NULL (default: now())
total_sensors               | integer                  | NOT NULL (default: 0)
active_sensors              | integer                  | NOT NULL (default: 0)
total_actuators             | integer                  | NOT NULL (default: 0)
active_actuators            | integer                  | NOT NULL (default: 0)
open_alerts                 | integer                  | NOT NULL (default: 0)
critical_alerts             | integer                  | NOT NULL (default: 0)
average_temperature         | numeric(5,2)             | NULL
average_humidity            | numeric(5,2)             | NULL
average_soil_moisture       | numeric(5,2)             | NULL
average_light_intensity     | numeric(10,2)            | NULL
average_co2_level           | numeric(7,2)             | NULL
min_temperature             | numeric(5,2)             | NULL
max_temperature             | numeric(5,2)             | NULL
min_humidity                | numeric(5,2)             | NULL
max_humidity                | numeric(5,2)             | NULL
actuators_on                | integer                  | NOT NULL (default: 0)
actuators_off               | integer                  | NOT NULL (default: 0)
actuators_auto              | integer                  | NOT NULL (default: 0)
actuators_manual            | integer                  | NOT NULL (default: 0)
last_sensor_reading_at      | timestamptz              | NULL
last_actuator_command_at    | timestamptz              | NULL
data_quality_score          | numeric(3,2)             | NULL
mqtt_connection_status      | varchar(20)              | NULL
notes                       | text                     | NULL
metadata                    | jsonb                    | NULL
created_at                  | timestamptz              | NULL (default: now())
updated_at                  | timestamptz              | NULL (default: now())
```

---

## SOLUCIÓN: Ignorar campos ARRAY en entidades @Immutable

**Para entidades readonly (@Immutable), la solución MÁS SIMPLE es:**

```kotlin
@Entity
@Table(name = "alert_resolution_history", schema = "metadata")
@Immutable
data class AlertResolutionHistory(
    @Id
    val id: Long,

    // ... otros campos ...

    // ⚠️ NO mapear este campo - causa problemas con Hibernate validation
    // @Column(name = "actions_taken")
    // val actionsTaken: Array<String>? = null,

    // ... otros campos ...
)
```

**Si realmente necesitas el campo:**
1. Agregar dependencia: `implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")`
2. Usar `@Type` annotation correctamente

---

## CONFIGURACIÓN HIBERNATE

**IMPORTANTE:** DDL mode está en `validate` en ambos datasources:

```kotlin
// PostGreSQLDataSourceConfig.kt y TimescaleDataSourceConfig.kt
val properties = hashMapOf<String, Any>(
    "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
    "hibernate.hbm2ddl.auto" to "validate",  // ⚠️ VALIDA contra DB real
    // ...
)
```

Esto significa que **TODO debe coincidir EXACTAMENTE** con la base de datos.

---

## RECOMENDACIÓN FINAL

**Para las 7 entidades readonly creadas:**
- ✅ Eliminar campos con PostgreSQL ARRAY types
- ✅ Mantener solo campos con tipos estándar
- ✅ Dejar campos ARRAY sin mapear (no son necesarios para readonly)

Esto evita problemas de Hibernate validation sin necesidad de librerías adicionales.
