package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.sector.dto.SectorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/greenhouses/{greenhouseId}/sectors")
@Tag(name = "Greenhouse Sector Management", description = "Endpoints para la gestión de sectores de un invernadero")
class GreenhouseSectorController(
    private val sectorService: SectorService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los sectores de un invernadero")
    fun getAllByGreenhouseId(@PathVariable greenhouseId: UUID): ResponseEntity<List<SectorResponse>> {
        return ResponseEntity.ok(sectorService.findAllByGreenhouseId(greenhouseId))
    }
}
