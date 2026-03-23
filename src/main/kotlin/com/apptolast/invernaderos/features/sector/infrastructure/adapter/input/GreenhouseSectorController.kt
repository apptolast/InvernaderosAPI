package com.apptolast.invernaderos.features.sector.infrastructure.adapter.input

import com.apptolast.invernaderos.features.sector.domain.port.input.FindSectorUseCase
import com.apptolast.invernaderos.features.sector.dto.mapper.toResponse
import com.apptolast.invernaderos.features.sector.dto.response.SectorResponse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/greenhouses/{greenhouseId}/sectors")
@Tag(name = "Greenhouse Sector Management", description = "Endpoints para la gestión de sectores de un invernadero")
class GreenhouseSectorController(
    private val findUseCase: FindSectorUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los sectores de un invernadero")
    fun getAllByGreenhouseId(@PathVariable greenhouseId: Long): ResponseEntity<List<SectorResponse>> {
        val sectors = findUseCase.findAllByGreenhouseId(GreenhouseId(greenhouseId))
        return ResponseEntity.ok(sectors.map { it.toResponse() })
    }
}
