package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.time.Instant

/**
 * Ultimo valor conocido por code de device/setting.
 *
 * Tabla tiny (una fila por code) que se actualiza via UPSERT en cada mensaje MQTT.
 * El WebSocket assembler lee de aqui en vez de hacer DISTINCT ON sobre sensor_readings.
 *
 * @property code Codigo del device o setting (e.g., SET-00036, DEV-00031)
 * @property value Ultimo valor conocido como string
 * @property firstSeenAt Primera vez que se vio este code
 * @property lastSeenAt Ultima actualizacion (confirma liveness del sensor)
 * @property updateCount Numero total de actualizaciones recibidas
 */
@Entity
@Table(name = "device_current_values", schema = "iot")
data class DeviceCurrentValue(
    @Id
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    val value: String,

    @Column(name = "first_seen_at", nullable = false)
    val firstSeenAt: Instant = Instant.now(),

    @Column(name = "last_seen_at", nullable = false)
    val lastSeenAt: Instant = Instant.now(),

    @Column(name = "update_count", nullable = false)
    val updateCount: Long = 1
)
