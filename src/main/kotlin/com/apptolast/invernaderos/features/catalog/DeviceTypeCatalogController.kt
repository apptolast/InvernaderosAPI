package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.DeviceTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.DeviceTypeUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.DeviceTypeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/device-types")
@Tag(name = "Catalog - Device Types", description = "CRUD de tipos de dispositivos")
class DeviceTypeCatalogController(
    private val deviceTypeService: DeviceTypeService
) {

    @GetMapping
    @Operation(summary = "Obtener tipos de dispositivos")
    fun getAll(
        @RequestParam(required = false) categoryId: Short?,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean
    ): ResponseEntity<List<DeviceTypeResponse>> {
        return ResponseEntity.ok(deviceTypeService.findAll(categoryId, activeOnly))
    }

    @GetMapping("/sensors")
    @Operation(summary = "Obtener tipos de sensores")
    fun getSensorTypes(): ResponseEntity<List<DeviceTypeResponse>> {
        return ResponseEntity.ok(deviceTypeService.findSensorTypes())
    }

    @GetMapping("/actuators")
    @Operation(summary = "Obtener tipos de actuadores")
    fun getActuatorTypes(): ResponseEntity<List<DeviceTypeResponse>> {
        return ResponseEntity.ok(deviceTypeService.findActuatorTypes())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tipo de dispositivo por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<DeviceTypeResponse> {
        val type = deviceTypeService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(type)
    }

    @PostMapping
    @Operation(summary = "Crear nuevo tipo de dispositivo")
    fun create(@Valid @RequestBody request: DeviceTypeCreateRequest): ResponseEntity<DeviceTypeResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(deviceTypeService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de dispositivo")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: DeviceTypeUpdateRequest): ResponseEntity<DeviceTypeResponse> {
        return try {
            val updated = deviceTypeService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tipo de dispositivo")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (deviceTypeService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar tipo de dispositivo")
    fun deactivate(@PathVariable id: Short): ResponseEntity<DeviceTypeResponse> {
        val deactivated = deviceTypeService.deactivate(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(deactivated)
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar tipo de dispositivo")
    fun activate(@PathVariable id: Short): ResponseEntity<DeviceTypeResponse> {
        val activated = deviceTypeService.activate(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(activated)
    }
}
