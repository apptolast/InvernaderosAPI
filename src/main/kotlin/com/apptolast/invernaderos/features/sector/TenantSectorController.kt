package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.sector.dto.SectorCreateRequest
import com.apptolast.invernaderos.features.sector.dto.SectorResponse
import com.apptolast.invernaderos.features.sector.dto.SectorUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/sectors")
@Tag(name = "Tenant Sector Management", description = "Endpoints para la gestión de sectores de un cliente")
class TenantSectorController(
    private val sectorService: SectorService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los sectores de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<SectorResponse>> {
        return ResponseEntity.ok(sectorService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{sectorId}")
    @Operation(summary = "Obtener un sector específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long
    ): ResponseEntity<SectorResponse> {
        val sector = sectorService.findByIdAndTenantId(sectorId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sector)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo sector para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: SectorCreateRequest
    ): ResponseEntity<SectorResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.create(tenantId, request))
    }

    @PutMapping("/{sectorId}")
    @Operation(summary = "Actualizar un sector existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long,
        @RequestBody request: SectorUpdateRequest
    ): ResponseEntity<SectorResponse> {
        val updated = sectorService.update(sectorId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{sectorId}")
    @Operation(summary = "Eliminar un sector de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long
    ): ResponseEntity<Unit> {
        return if (sectorService.delete(sectorId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
