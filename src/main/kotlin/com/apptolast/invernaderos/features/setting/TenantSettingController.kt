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
 * Los settings definen rangos min/max para cada tipo de parámetro (sensor)
 * por invernadero y periodo del día.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html">Spring Boot Web MVC</a>
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/settings")
@Tag(name = "Tenant Setting Management", description = "Endpoints para la gestión de configuraciones de parámetros de un cliente")
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
    @Operation(summary = "Obtener una configuración específica de un cliente")
    fun getById(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuración") @PathVariable settingId: Long
    ): ResponseEntity<SettingResponse> {
        val setting = settingService.findByIdAndTenantId(settingId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(setting)
    }

    @PostMapping
    @Operation(summary = "Crear una nueva configuración para un cliente")
    fun create(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Valid @RequestBody request: SettingCreateRequest
    ): ResponseEntity<SettingResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(settingService.create(tenantId, request))
    }

    @PutMapping("/{settingId}")
    @Operation(summary = "Actualizar una configuración existente de un cliente")
    fun update(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuración") @PathVariable settingId: Long,
        @Valid @RequestBody request: SettingUpdateRequest
    ): ResponseEntity<SettingResponse> {
        val updated = settingService.update(settingId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{settingId}")
    @Operation(summary = "Eliminar una configuración de un cliente")
    fun delete(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID de la configuración") @PathVariable settingId: Long
    ): ResponseEntity<Unit> {
        return if (settingService.delete(settingId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Endpoints adicionales para filtrar por invernadero

    @GetMapping("/greenhouse/{greenhouseId}")
    @Operation(summary = "Obtener todas las configuraciones de un invernadero")
    fun getByGreenhouseId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del invernadero") @PathVariable greenhouseId: Long
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findAllByGreenhouseId(greenhouseId))
    }

    @GetMapping("/greenhouse/{greenhouseId}/active")
    @Operation(summary = "Obtener las configuraciones activas de un invernadero")
    fun getActiveByGreenhouseId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del invernadero") @PathVariable greenhouseId: Long
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findActiveByGreenhouseId(greenhouseId))
    }

    @GetMapping("/greenhouse/{greenhouseId}/parameter/{parameterId}")
    @Operation(summary = "Obtener las configuraciones de un invernadero filtradas por tipo de parámetro")
    fun getByGreenhouseIdAndParameterId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del invernadero") @PathVariable greenhouseId: Long,
        @Parameter(description = "ID del tipo de parámetro (device_type)") @PathVariable parameterId: Short
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findByGreenhouseIdAndParameterId(greenhouseId, parameterId))
    }

    @GetMapping("/greenhouse/{greenhouseId}/period/{periodId}")
    @Operation(summary = "Obtener las configuraciones de un invernadero filtradas por periodo")
    fun getByGreenhouseIdAndPeriodId(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del invernadero") @PathVariable greenhouseId: Long,
        @Parameter(description = "ID del periodo (1=DAY, 2=NIGHT, 3=ALL)") @PathVariable periodId: Short
    ): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.findByGreenhouseIdAndPeriodId(greenhouseId, periodId))
    }

    @GetMapping("/greenhouse/{greenhouseId}/parameter/{parameterId}/period/{periodId}")
    @Operation(summary = "Obtener una configuración específica por invernadero, parámetro y periodo")
    fun getByGreenhouseParameterAndPeriod(
        @Parameter(description = "ID del tenant") @PathVariable tenantId: Long,
        @Parameter(description = "ID del invernadero") @PathVariable greenhouseId: Long,
        @Parameter(description = "ID del tipo de parámetro") @PathVariable parameterId: Short,
        @Parameter(description = "ID del periodo") @PathVariable periodId: Short
    ): ResponseEntity<SettingResponse> {
        val setting = settingService.findByGreenhouseParameterAndPeriod(greenhouseId, parameterId, periodId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(setting)
    }
}
