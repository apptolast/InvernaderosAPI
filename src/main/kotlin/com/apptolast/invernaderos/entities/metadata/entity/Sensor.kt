package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sensors", schema = "metadata")
data class Sensor(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "device_id", nullable = false, length = 50)
    val deviceId: String,

    @Column(name = "sensor_type", nullable = false, length = 50)
    val sensorType: String,

    @Column(length = 20)
    val unit: String? = null,

    @Column(name = "min_threshold" , precision = 10, scale = 2)
    val minThreshold: BigDecimal? = null,

    @Column(name = "max_threshold" , precision = 10, scale = 2)
    val maxThreshold: BigDecimal? = null,

    @Column(name = "location_in_greenhouse", length = 100)
    val locationInGreenhouse: String? = null,

    @Column(name = "calibration_data", columnDefinition = "jsonb")
    val calibrationData: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "last_seen")
    val lastSeen: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)