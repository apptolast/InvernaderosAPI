package com.apptolast.invernaderos.entities.timescaledb.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "sensor_readings", schema = "public")
data class SensorReading(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Column(name = "greenhouse_id", length = 50)
    val greenhouseId: String? = null,

    @Column(name = "sensor_id", nullable = false, length = 50)
    val sensorId: String,

    @Column(name = "sensor_type", nullable = false, length = 30)
    val sensorType: String,

    @Column(nullable = false, columnDefinition = "double precision")
    val value: Double,

    @Column(length = 20)
    val unit: String? = null
) {
    // Composite key para TimescaleDB
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorReading) return false
        return time == other.time && sensorId == other.sensorId
    }

    override fun hashCode(): Int {
        return 31 * time.hashCode() + sensorId.hashCode()
    }
}
