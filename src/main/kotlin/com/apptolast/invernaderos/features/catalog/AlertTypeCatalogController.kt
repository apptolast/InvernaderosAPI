package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.AlertTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.AlertTypeUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.AlertTypeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/alert-types")
@Tag(name = "Catalog - Alert Types", description = "CRUD de tipos de alerta")
class AlertTypeCatalogController(
    private val alertTypeService: AlertTypeService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los tipos de alerta")
    fun getAll(): ResponseEntity<List<AlertTypeResponse>> {
        return ResponseEntity.ok(alertTypeService.findAll())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tipo de alerta por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<AlertTypeResponse> {
        val type = alertTypeService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(type)
    }

    @PostMapping
    @Operation(summary = "Crear nuevo tipo de alerta")
    fun create(@Valid @RequestBody request: AlertTypeCreateRequest): ResponseEntity<AlertTypeResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(alertTypeService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de alerta")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: AlertTypeUpdateRequest): ResponseEntity<AlertTypeResponse> {
        return try {
            val updated = alertTypeService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tipo de alerta")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (alertTypeService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}
