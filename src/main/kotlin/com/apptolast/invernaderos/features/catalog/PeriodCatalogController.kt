package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.PeriodCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.PeriodUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.PeriodResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/periods")
@Tag(name = "Catalog - Periods", description = "CRUD de periodos")
class PeriodCatalogController(
    private val periodService: PeriodService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los periodos")
    fun getAll(): ResponseEntity<List<PeriodResponse>> {
        return ResponseEntity.ok(periodService.findAll())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un periodo por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<PeriodResponse> {
        val period = periodService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(period)
    }

    @PostMapping
    @Operation(summary = "Crear nuevo periodo")
    fun create(@Valid @RequestBody request: PeriodCreateRequest): ResponseEntity<PeriodResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(periodService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar periodo")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: PeriodUpdateRequest): ResponseEntity<PeriodResponse> {
        return try {
            val updated = periodService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar periodo")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (periodService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}
