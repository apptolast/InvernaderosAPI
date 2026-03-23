package com.apptolast.invernaderos.features.device.infrastructure.adapter.input

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.DeleteDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.FindCommandHistoryUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.FindDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceUseCase
import com.apptolast.invernaderos.features.device.dto.mapper.toCommand
import com.apptolast.invernaderos.features.device.dto.mapper.toResponse
import com.apptolast.invernaderos.features.device.dto.request.DeviceCreateRequest
import com.apptolast.invernaderos.features.device.dto.request.DeviceUpdateRequest
import com.apptolast.invernaderos.features.device.dto.response.CommandHistoryResponse
import com.apptolast.invernaderos.features.device.dto.response.DeviceResponse
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/devices")
@Tag(name = "Tenant Device Management", description = "Endpoints para la gestión de dispositivos de un cliente")
class TenantDeviceController(
    private val createUseCase: CreateDeviceUseCase,
    private val findUseCase: FindDeviceUseCase,
    private val updateUseCase: UpdateDeviceUseCase,
    private val deleteUseCase: DeleteDeviceUseCase,
    private val findCommandHistoryUseCase: FindCommandHistoryUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los dispositivos de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<DeviceResponse>> {
        val devices = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(devices.map { it.toResponse() })
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Obtener un dispositivo específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<DeviceResponse> {
        return findUseCase.findByIdAndTenantId(DeviceId(deviceId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo dispositivo para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: DeviceCreateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(TenantId(tenantId))
        return createUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is DeviceError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is DeviceError.NotFound ->
                        ResponseEntity.notFound().build()
                }
            },
            onRight = { device ->
                ResponseEntity.status(HttpStatus.CREATED).body(device.toResponse())
            }
        )
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "Actualizar un dispositivo existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long,
        @RequestBody request: DeviceUpdateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(DeviceId(deviceId), TenantId(tenantId))
        return updateUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is DeviceError.NotFound ->
                        ResponseEntity.notFound().build()
                    is DeviceError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                }
            },
            onRight = { device ->
                ResponseEntity.ok(device.toResponse())
            }
        )
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Eliminar un dispositivo de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(DeviceId(deviceId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }

    @GetMapping("/{deviceId}/commands")
    @Operation(summary = "Obtener historial de comandos de un dispositivo")
    fun getCommandHistory(
        @PathVariable tenantId: Long,
        @PathVariable deviceId: Long,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<CommandHistoryResponse>> {
        val history = findCommandHistoryUseCase.findRecentByDevice(DeviceId(deviceId), limit)
        return ResponseEntity.ok(history.map { it.toResponse() })
    }
}
