package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.DataTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.DataTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.DataTypeUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para operaciones CRUD de tipos de datos.
 * Los tipos de datos definen como interpretar los valores en Settings
 * (INTEGER, BOOLEAN, STRING, DOUBLE, DATE, TIME, DATETIME, JSON, LONG)
 */
@RestController
@RequestMapping("/api/v1/catalog/data-types")
@Tag(name = "Data Types Catalog", description = "Catalogo de tipos de datos para configuraciones")
class DataTypeController(
    private val dataTypeService: DataTypeService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los tipos de datos ordenados por displayOrder")
    fun getAll(): ResponseEntity<List<DataTypeResponse>> {
        return ResponseEntity.ok(dataTypeService.findAll())
    }

    @GetMapping("/active")
    @Operation(summary = "Obtener solo los tipos de datos activos")
    fun getAllActive(): ResponseEntity<List<DataTypeResponse>> {
        return ResponseEntity.ok(dataTypeService.findAllActive())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tipo de dato por ID")
    fun getById(
        @Parameter(description = "ID del tipo de dato") @PathVariable id: Short
    ): ResponseEntity<DataTypeResponse> {
        val dataType = dataTypeService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dataType)
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Obtener un tipo de dato por nombre")
    fun getByName(
        @Parameter(description = "Nombre del tipo de dato", example = "INTEGER") @PathVariable name: String
    ): ResponseEntity<DataTypeResponse> {
        val dataType = dataTypeService.findByName(name)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dataType)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo tipo de dato")
    fun create(
        @Valid @RequestBody request: DataTypeCreateRequest
    ): ResponseEntity<DataTypeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataTypeService.create(request))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un tipo de dato existente")
    fun update(
        @Parameter(description = "ID del tipo de dato") @PathVariable id: Short,
        @Valid @RequestBody request: DataTypeUpdateRequest
    ): ResponseEntity<DataTypeResponse> {
        val updated = dataTypeService.update(id, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un tipo de dato (no permite eliminar tipos basicos del sistema)")
    fun delete(
        @Parameter(description = "ID del tipo de dato") @PathVariable id: Short
    ): ResponseEntity<Unit> {
        return if (dataTypeService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/validate")
    @Operation(summary = "Validar si un valor es valido para un tipo de dato")
    fun validateValue(
        @Parameter(description = "ID del tipo de dato") @PathVariable id: Short,
        @Parameter(description = "Valor a validar") @RequestParam value: String
    ): ResponseEntity<Map<String, Any>> {
        val isValid = dataTypeService.validateValue(id, value)
        return ResponseEntity.ok(mapOf(
            "dataTypeId" to id,
            "value" to value,
            "isValid" to isValid
        ))
    }
}
