package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.AlertSeverityCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.AlertSeverityUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.AlertSeverityResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/alert-severities")
@Tag(name = "Catalog - Alert Severities", description = "CRUD de niveles de severidad")
class AlertSeverityCatalogController(
    private val alertSeverityService: AlertSeverityService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los niveles de severidad")
    fun getAll(): ResponseEntity<List<AlertSeverityResponse>> {
        return ResponseEntity.ok(alertSeverityService.findAll())
    }

    @GetMapping("/critical")
    @Operation(summary = "Obtener severidades que requieren acción")
    fun getCritical(): ResponseEntity<List<AlertSeverityResponse>> {
        return ResponseEntity.ok(alertSeverityService.findRequiringAction())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un nivel de severidad por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<AlertSeverityResponse> {
        val severity = alertSeverityService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(severity)
    }

    @PostMapping
    @Operation(summary = "Crear nuevo nivel de severidad")
    fun create(@Valid @RequestBody request: AlertSeverityCreateRequest): ResponseEntity<AlertSeverityResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(alertSeverityService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nivel de severidad")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: AlertSeverityUpdateRequest): ResponseEntity<AlertSeverityResponse> {
        return try {
            val updated = alertSeverityService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar nivel de severidad")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (alertSeverityService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}
