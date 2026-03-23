package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.ActuatorStateCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.ActuatorStateUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.ActuatorStateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/actuator-states")
@Tag(name = "Catalog - Actuator States", description = "CRUD de estados de actuadores")
class ActuatorStateCatalogController(
    private val actuatorStateService: ActuatorStateService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los estados de actuadores")
    fun getAll(): ResponseEntity<List<ActuatorStateResponse>> {
        return ResponseEntity.ok(actuatorStateService.findAll())
    }

    @GetMapping("/operational")
    @Operation(summary = "Obtener estados operacionales")
    fun getOperational(): ResponseEntity<List<ActuatorStateResponse>> {
        return ResponseEntity.ok(actuatorStateService.findOperational())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un estado por ID")
    fun getById(@PathVariable id: Short): ResponseEntity<ActuatorStateResponse> {
        val state = actuatorStateService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(state)
    }

    @PostMapping
    @Operation(summary = "Crear nuevo estado de actuador")
    fun create(@Valid @RequestBody request: ActuatorStateCreateRequest): ResponseEntity<ActuatorStateResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(actuatorStateService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado de actuador")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: ActuatorStateUpdateRequest): ResponseEntity<ActuatorStateResponse> {
        return try {
            val updated = actuatorStateService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estado de actuador")
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        val deleted = actuatorStateService.delete(id)
        return if (deleted) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}
