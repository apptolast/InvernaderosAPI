package com.apptolast.invernaderos.features.sector.infrastructure.adapter.input

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.DeleteSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.FindSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorUseCase
import com.apptolast.invernaderos.features.sector.dto.mapper.toCommand
import com.apptolast.invernaderos.features.sector.dto.mapper.toResponse
import com.apptolast.invernaderos.features.sector.dto.request.SectorCreateRequest
import com.apptolast.invernaderos.features.sector.dto.request.SectorUpdateRequest
import com.apptolast.invernaderos.features.sector.dto.response.SectorResponse
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/sectors")
@Tag(name = "Tenant Sector Management", description = "Endpoints para la gestión de sectores de un cliente")
class TenantSectorController(
    private val createUseCase: CreateSectorUseCase,
    private val findUseCase: FindSectorUseCase,
    private val updateUseCase: UpdateSectorUseCase,
    private val deleteUseCase: DeleteSectorUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los sectores de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<SectorResponse>> {
        val sectors = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(sectors.map { it.toResponse() })
    }

    @GetMapping("/{sectorId}")
    @Operation(summary = "Obtener un sector específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long
    ): ResponseEntity<SectorResponse> {
        return findUseCase.findByIdAndTenantId(SectorId(sectorId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo sector para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: SectorCreateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(TenantId(tenantId))
        return createUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is SectorError.GreenhouseNotFound ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is SectorError.GreenhouseNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is SectorError.NotFound ->
                        ResponseEntity.notFound().build()
                }
            },
            onRight = { sector ->
                ResponseEntity.status(HttpStatus.CREATED).body(sector.toResponse())
            }
        )
    }

    @PutMapping("/{sectorId}")
    @Operation(summary = "Actualizar un sector existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long,
        @RequestBody request: SectorUpdateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(SectorId(sectorId), TenantId(tenantId))
        return updateUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is SectorError.NotFound ->
                        ResponseEntity.notFound().build()
                    is SectorError.GreenhouseNotFound ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is SectorError.GreenhouseNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                }
            },
            onRight = { sector ->
                ResponseEntity.ok(sector.toResponse())
            }
        )
    }

    @DeleteMapping("/{sectorId}")
    @Operation(summary = "Eliminar un sector de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(SectorId(sectorId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }
}
