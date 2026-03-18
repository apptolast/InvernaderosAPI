package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.time.Instant

/**
 * Archivo fiel de lecturas de sensores sin deduplicacion.
 *
 * Cada mensaje MQTT se inserta aqui sin filtrar.
 * Tabla grande, comprimida tras 3 dias, retencion 2 anios.
 * Solo se consulta para auditoria o analisis detallado.
 *
 * Reutiliza SensorReadingId como clave compuesta (time, code).
 *
 * @property time Timestamp de la lectura
 * @property code Codigo del device o setting (e.g., DEV-00038)
 * @property value Valor como string
 */
@Entity
@Table(name = "sensor_readings_raw", schema = "iot")
@IdClass(SensorReadingId::class)
data class SensorReadingRaw(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    val value: String
)
