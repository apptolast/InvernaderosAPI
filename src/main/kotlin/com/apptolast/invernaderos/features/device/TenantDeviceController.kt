package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.device.dto.DeviceCreateRequest
import com.apptolast.invernaderos.features.device.dto.DeviceResponse
import com.apptolast.invernaderos.features.device.dto.DeviceUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/devices")
@Tag(name = "Tenant Device Management", description = "Endpoints para la gestión de dispositivos de un cliente")
class TenantDeviceController(
    private val deviceService: DeviceService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los dispositivos de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<DeviceResponse>> {
        return ResponseEntity.ok(deviceService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Obtener un dispositivo específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<DeviceResponse> {
        val device = deviceService.findByIdAndTenantId(deviceId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(device)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo dispositivo para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: DeviceCreateRequest
    ): ResponseEntity<DeviceResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.create(tenantId, request))
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "Actualizar un dispositivo existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long,
        @RequestBody request: DeviceUpdateRequest
    ): ResponseEntity<DeviceResponse> {
        val updated = deviceService.update(deviceId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Eliminar un dispositivo de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<Unit> {
        return if (deviceService.delete(deviceId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
