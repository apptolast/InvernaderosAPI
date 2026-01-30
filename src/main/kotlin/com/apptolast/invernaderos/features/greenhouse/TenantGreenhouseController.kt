package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseCreateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseResponse
import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/greenhouses")
@Tag(name = "Tenant Greenhouse Management", description = "Endpoints para la gestión de invernaderos de un cliente")
class TenantGreenhouseController(
    private val greenhouseService: GreenhouseService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los invernaderos de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<GreenhouseResponse>> {
        return ResponseEntity.ok(greenhouseService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{greenhouseId}")
    @Operation(summary = "Obtener un invernadero específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long
    ): ResponseEntity<GreenhouseResponse> {
        val greenhouse = greenhouseService.findByIdAndTenantId(greenhouseId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(greenhouse)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo invernadero para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: GreenhouseCreateRequest
    ): ResponseEntity<GreenhouseResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(greenhouseService.create(tenantId, request))
    }

    @PutMapping("/{greenhouseId}")
    @Operation(summary = "Actualizar un invernadero existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long,
        @RequestBody request: GreenhouseUpdateRequest
    ): ResponseEntity<GreenhouseResponse> {
        val updated = greenhouseService.update(greenhouseId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{greenhouseId}")
    @Operation(summary = "Eliminar un invernadero de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long
    ): ResponseEntity<Unit> {
        return if (greenhouseService.delete(greenhouseId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
