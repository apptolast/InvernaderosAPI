package com.apptolast.invernaderos.features.setting.infrastructure.adapter.input

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.DeleteSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.FindSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingUseCase
import com.apptolast.invernaderos.features.setting.dto.mapper.toCommand
import com.apptolast.invernaderos.features.setting.dto.mapper.toResponse
import com.apptolast.invernaderos.features.setting.dto.request.SettingCreateRequest
import com.apptolast.invernaderos.features.setting.dto.request.SettingUpdateRequest
import com.apptolast.invernaderos.features.setting.dto.response.SettingResponse
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/settings")
@Tag(name = "Tenant Setting Management", description = "Endpoints para la gestion de configuraciones de parametros de un cliente")
class TenantSettingController(
    private val createUseCase: CreateSettingUseCase,
    private val findUseCase: FindSettingUseCase,
    private val updateUseCase: UpdateSettingUseCase,
    private val deleteUseCase: DeleteSettingUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todas las configuraciones de un cliente")
    fun getAllByTenantId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long
    ): ResponseEntity<List<SettingResponse>> {
        val settings = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(settings.map { it.toResponse() })
    }

    @GetMapping("/{settingId}")
    @Operation(summary = "Obtener una configuracion especifica de un cliente")
    fun getById(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long
    ): ResponseEntity<SettingResponse> {
        return findUseCase.findByIdAndTenantId(SettingId(settingId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear una nueva configuracion para un cliente")
    fun create(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Valid @RequestBody request: SettingCreateRequest
    ): ResponseEntity<Any> {
        return createUseCase.execute(request.toCommand(TenantId(tenantId))).fold(
            onLeft = { error ->
                when (error) {
                    is SettingError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is SettingError.NotFound ->
                        ResponseEntity.notFound().build()
                }
            },
            onRight = { setting ->
                ResponseEntity.status(HttpStatus.CREATED).body(setting.toResponse())
            }
        )
    }

    @PutMapping("/{settingId}")
    @Operation(summary = "Actualizar una configuracion existente de un cliente")
    fun update(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long,
        @Valid @RequestBody request: SettingUpdateRequest
    ): ResponseEntity<Any> {
        return updateUseCase.execute(request.toCommand(SettingId(settingId), TenantId(tenantId))).fold(
            onLeft = { error ->
                when (error) {
                    is SettingError.NotFound ->
                        ResponseEntity.notFound().build()
                    is SettingError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                }
            },
            onRight = { setting ->
                ResponseEntity.ok(setting.toResponse())
            }
        )
    }

    @DeleteMapping("/{settingId}")
    @Operation(summary = "Eliminar una configuracion de un cliente")
    fun delete(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(SettingId(settingId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }

    // Endpoints adicionales para filtrar por sector

    @GetMapping("/sector/{sectorId}")
    @Operation(summary = "Obtener todas las configuraciones de un sector")
    fun getBySectorId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long
    ): ResponseEntity<List<SettingResponse>> {
        val settings = findUseCase.findAllBySectorId(SectorId(sectorId))
        return ResponseEntity.ok(settings.map { it.toResponse() })
    }

    @GetMapping("/sector/{sectorId}/active")
    @Operation(summary = "Obtener las configuraciones activas de un sector")
    fun getActiveBySectorId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long
    ): ResponseEntity<List<SettingResponse>> {
        val settings = findUseCase.findActiveBySectorId(SectorId(sectorId))
        return ResponseEntity.ok(settings.map { it.toResponse() })
    }

    @GetMapping("/sector/{sectorId}/parameter/{parameterId}")
    @Operation(summary = "Obtener las configuraciones de un sector filtradas por tipo de parametro")
    fun getBySectorIdAndParameterId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del tipo de parametro (device_type)") @PathVariable parameterId: Short
    ): ResponseEntity<List<SettingResponse>> {
        val settings = findUseCase.findBySectorIdAndParameterId(SectorId(sectorId), parameterId)
        return ResponseEntity.ok(settings.map { it.toResponse() })
    }

    @GetMapping("/sector/{sectorId}/actuator-state/{actuatorStateId}")
    @Operation(summary = "Obtener las configuraciones de un sector filtradas por estado de actuador")
    fun getBySectorIdAndActuatorStateId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del estado del actuador (1=OFF, 2=ON, 3=AUTO, etc.)") @PathVariable actuatorStateId: Short
    ): ResponseEntity<List<SettingResponse>> {
        val settings = findUseCase.findBySectorIdAndActuatorStateId(SectorId(sectorId), actuatorStateId)
        return ResponseEntity.ok(settings.map { it.toResponse() })
    }

    @GetMapping("/sector/{sectorId}/parameter/{parameterId}/actuator-state/{actuatorStateId}")
    @Operation(summary = "Obtener una configuracion especifica por sector, parametro y estado de actuador")
    fun getBySectorParameterAndActuatorState(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del tipo de parametro") @PathVariable parameterId: Short,
        @Parameter(description = "ID del estado del actuador") @PathVariable actuatorStateId: Short
    ): ResponseEntity<SettingResponse> {
        return findUseCase.findBySectorParameterAndActuatorState(SectorId(sectorId), parameterId, actuatorStateId).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }
}
