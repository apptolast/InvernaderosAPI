package com.apptolast.invernaderos.features.sensor

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SensorRepository : JpaRepository<Sensor, UUID> {
    @EntityGraph(value = "Sensor.context") fun findByGreenhouseId(greenhouseId: UUID): List<Sensor>

    @EntityGraph(value = "Sensor.context") fun findByDeviceId(deviceId: String): Sensor?

    fun findByIsActive(isActive: Boolean): List<Sensor>
}
