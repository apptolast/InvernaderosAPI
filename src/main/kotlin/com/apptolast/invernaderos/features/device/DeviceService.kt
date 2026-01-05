package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.device.dto.DeviceCreateRequest
import com.apptolast.invernaderos.features.device.dto.DeviceResponse
import com.apptolast.invernaderos.features.device.dto.DeviceUpdateRequest
import com.apptolast.invernaderos.features.device.dto.toResponse
import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val greenhouseRepository: GreenhouseRepository
) {

    fun findAllByTenantId(tenantId: UUID): List<DeviceResponse> {
        return deviceRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    fun findAllByGreenhouseId(greenhouseId: UUID): List<DeviceResponse> {
        return deviceRepository.findByGreenhouseId(greenhouseId).map { it.toResponse() }
    }

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): DeviceResponse? {
        val device = deviceRepository.findById(id).orElse(null) ?: return null
        if (device.tenantId != tenantId) return null
        return device.toResponse()
    }

    /**
     * Crea un nuevo dispositivo.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     * Ref: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html
     */
    @Transactional
    fun create(tenantId: UUID, request: DeviceCreateRequest): DeviceResponse {
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("Invernadero no encontrado")

        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al cliente especificado")
        }

        val device = Device(
            tenantId = tenantId,
            greenhouseId = request.greenhouseId,
            name = request.name?.trim(),
            categoryId = request.categoryId,
            typeId = request.typeId,
            unitId = request.unitId,
            isActive = request.isActive ?: true
        )

        val savedDevice = deviceRepository.save(device)
        // Reload with EntityGraph to load lazy relations (category, type, unit)
        return deviceRepository.findById(savedDevice.id!!).orElseThrow().toResponse()
    }

    /**
     * Actualiza un dispositivo existente.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     */
    @Transactional
    fun update(id: UUID, tenantId: UUID, request: DeviceUpdateRequest): DeviceResponse? {
        val device = deviceRepository.findById(id).orElse(null) ?: return null
        if (device.tenantId != tenantId) return null

        val updatedDevice = device.copy(
            name = request.name?.trim() ?: device.name,
            categoryId = request.categoryId ?: device.categoryId,
            typeId = request.typeId ?: device.typeId,
            unitId = request.unitId ?: device.unitId,
            isActive = request.isActive ?: device.isActive,
            updatedAt = Instant.now()
        )

        deviceRepository.save(updatedDevice)
        // Reload with EntityGraph to load lazy relations (category, type, unit)
        return deviceRepository.findById(id).orElseThrow().toResponse()
    }

    @Transactional
    fun delete(id: UUID, tenantId: UUID): Boolean {
        val device = deviceRepository.findById(id).orElse(null) ?: return false
        if (device.tenantId != tenantId) return false

        deviceRepository.delete(device)
        return true
    }
}
