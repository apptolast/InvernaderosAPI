package com.apptolast.invernaderos.features.device

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {

    @EntityGraph(value = "Device.context")
    fun findByGreenhouseId(greenhouseId: UUID): List<Device>

    @EntityGraph(value = "Device.context")
    fun findByTenantId(tenantId: UUID): List<Device>

    fun findByCategory(category: DeviceCategory): List<Device>

    fun findByGreenhouseIdAndCategory(greenhouseId: UUID, category: DeviceCategory): List<Device>

    fun findByCode(code: String): Device?

    fun findByGreenhouseIdAndCode(greenhouseId: UUID, code: String): Device?

    fun findByHardwareId(hardwareId: String): Device?

    fun findByIsActive(isActive: Boolean): List<Device>

    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Device>

    fun findByGreenhouseIdAndSector(greenhouseId: UUID, sector: String): List<Device>

    @Query("SELECT d FROM Device d WHERE d.greenhouseId = :greenhouseId AND d.category = 'SENSOR'")
    fun findSensorsByGreenhouseId(greenhouseId: UUID): List<Device>

    @Query("SELECT d FROM Device d WHERE d.greenhouseId = :greenhouseId AND d.category = 'ACTUATOR'")
    fun findActuatorsByGreenhouseId(greenhouseId: UUID): List<Device>

    @Query("SELECT d FROM Device d WHERE d.tenantId = :tenantId AND d.category = :category AND d.isActive = true")
    fun findActiveByTenantAndCategory(tenantId: UUID, category: DeviceCategory): List<Device>

    @Query("SELECT COUNT(d) FROM Device d WHERE d.greenhouseId = :greenhouseId AND d.isActive = true")
    fun countActiveByGreenhouseId(greenhouseId: UUID): Long

    @Query("SELECT d FROM Device d WHERE d.mqttTopic = :mqttTopic")
    fun findByMqttTopic(mqttTopic: String): Device?

    @Query("SELECT d FROM Device d WHERE d.mqttFieldName = :mqttFieldName AND d.greenhouseId = :greenhouseId")
    fun findByMqttFieldNameAndGreenhouseId(mqttFieldName: String, greenhouseId: UUID): Device?
}
