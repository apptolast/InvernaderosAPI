package com.apptolast.invernaderos.features.setting

import com.apptolast.invernaderos.features.setting.dto.SettingCreateRequest
import com.apptolast.invernaderos.features.setting.dto.SettingResponse
import com.apptolast.invernaderos.features.setting.dto.SettingUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para operaciones CRUD de settings asociados a un tenant.
 * Los settings definen valores de configuracion para cada tipo de parametro (sensor)
 * por sector y estado del actuador.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html">Spring Boot Web MVC</a>
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/settings")
@Tag(name = "Tenant Setting Management", description = "Endpoints para la gestion de configuraciones de parametros de un cliente")
class TenantSettingController(
    private val settingService: SettingService
) {

    @GetMapping
    @Operation(summary = "Obtener todas las configuraciones de un cliente")
    fun getAllByTenantId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{settingId}")
    @Operation(summary = "Obtener una configuracion especifica de un cliente")
    fun getById(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long
    ): ResponseEntity<SettingResponse> {
        val setting = settingService.findByIdAndTenantId(settingId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(setting)
    }

    @PostMapping
    @Operation(summary = "Crear una nueva configuracion para un cliente")
    fun create(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Valid @RequestBody request: SettingCreateRequest
    ): ResponseEntity<SettingResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(settingService.create(tenantId, request))
    }

    @PutMapping("/{settingId}")
    @Operation(summary = "Actualizar una configuracion existente de un cliente")
    fun update(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long,
        @Valid @RequestBody request: SettingUpdateRequest
    ): ResponseEntity<SettingResponse> {
        val updated = settingService.update(settingId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{settingId}")
    @Operation(summary = "Eliminar una configuracion de un cliente")
    fun delete(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuracion") @PathVariable settingId: Long
    ): ResponseEntity<Unit> {
        return if (settingService.delete(settingId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Endpoints adicionales para filtrar por sector

    @GetMapping("/sector/{sectorId}")
    @Operation(summary = "Obtener todas las configuraciones de un sector")
    fun getBySectorId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findAllBySectorId(sectorId))
    }

    @GetMapping("/sector/{sectorId}/active")
    @Operation(summary = "Obtener las configuraciones activas de un sector")
    fun getActiveBySectorId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findActiveBySectorId(sectorId))
    }

    @GetMapping("/sector/{sectorId}/parameter/{parameterId}")
    @Operation(summary = "Obtener las configuraciones de un sector filtradas por tipo de parametro")
    fun getBySectorIdAndParameterId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del tipo de parametro (device_type)") @PathVariable parameterId: Short
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findBySectorIdAndParameterId(sectorId, parameterId))
    }

    @GetMapping("/sector/{sectorId}/actuator-state/{actuatorStateId}")
    @Operation(summary = "Obtener las configuraciones de un sector filtradas por estado de actuador")
    fun getBySectorIdAndActuatorStateId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del estado del actuador (1=OFF, 2=ON, 3=AUTO, etc.)") @PathVariable actuatorStateId: Short
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findBySectorIdAndActuatorStateId(sectorId, actuatorStateId))
    }

    @GetMapping("/sector/{sectorId}/parameter/{parameterId}/actuator-state/{actuatorStateId}")
    @Operation(summary = "Obtener una configuracion especifica por sector, parametro y estado de actuador")
    fun getBySectorParameterAndActuatorState(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del sector") @PathVariable sectorId: Long,
        @Parameter(description = "ID del tipo de parametro") @PathVariable parameterId: Short,
        @Parameter(description = "ID del estado del actuador") @PathVariable actuatorStateId: Short
    ): ResponseEntity<SettingResponse> {
        val setting = settingService.findBySectorParameterAndActuatorState(sectorId, parameterId, actuatorStateId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(setting)
    }
}
