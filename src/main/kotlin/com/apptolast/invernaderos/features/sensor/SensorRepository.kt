package com.apptolast.invernaderos.features.sensor

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SensorRepository : JpaRepository<Sensor, UUID> {
    @EntityGraph(attributePaths = ["greenhouse"])
    fun findByGreenhouseId(greenhouseId: UUID): List<Sensor>

    @EntityGraph(attributePaths = ["greenhouse", "tenant"])
    fun findByDeviceId(deviceId: String): Sensor?

    fun findByIsActive(isActive: Boolean): List<Sensor>
}
