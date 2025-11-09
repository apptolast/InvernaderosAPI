package com.apptolast.invernaderos.repositories.metadata

import com.apptolast.invernaderos.entities.metadata.entity.Sensor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SensorRepository : JpaRepository<Sensor, UUID> {
    fun findByGreenhouseId(greenhouseId: UUID): List<Sensor>
    fun findByDeviceId(deviceId: String): Sensor?
    fun findByIsActive(isActive: Boolean): List<Sensor>
}