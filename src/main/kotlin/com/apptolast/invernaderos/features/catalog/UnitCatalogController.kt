package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.UnitCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.UnitUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.UnitResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/units")
@Tag(name = "Catalog - Units", description = "CRUD de unidades de medida")
class UnitCatalogController(
    private val unitService: UnitService
) {

    @GetMapping
    @Operation(summary = "Obtener todas las unidades de medida")
    fun getAll(@RequestParam(required = false, defaultValue = "true") activeOnly: Boolean): ResponseEntity<List<UnitResponse>> {
        return ResponseEntity.ok(unitService.findAll(activeOnly))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una unidad por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<UnitResponse> {
        val unit = unitService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(unit)
    }

    @PostMapping
    @Operation(summary = "Crear nueva unidad de medida")
    fun create(@Valid @RequestBody request: UnitCreateRequest): ResponseEntity<UnitResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar unidad de medida")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: UnitUpdateRequest): ResponseEntity<UnitResponse> {
        return try {
            val updated = unitService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar unidad de medida")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (unitService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar unidad de medida")
    fun activate(@PathVariable id: Short): ResponseEntity<UnitResponse> {
        val activated = unitService.activate(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(activated)
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar unidad de medida")
    fun deactivate(@PathVariable id: Short): ResponseEntity<UnitResponse> {
        val deactivated = unitService.deactivate(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(deactivated)
    }
}
