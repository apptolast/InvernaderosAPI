package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.request.DeviceCategoryCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.request.DeviceCategoryUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.response.DeviceCategoryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/catalog/device-categories")
@Tag(name = "Catalog - Device Categories", description = "CRUD de categorías de dispositivos")
class DeviceCategoryController(
    private val deviceCategoryService: DeviceCategoryService
) {

    @GetMapping
    @Operation(summary = "Obtener todas las categorías de dispositivos")
    fun getAll(): ResponseEntity<List<DeviceCategoryResponse>> {
        return ResponseEntity.ok(deviceCategoryService.findAll())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoría por ID")
    @ApiResponses(ApiResponse(responseCode = "200"), ApiResponse(responseCode = "404"))
    fun getById(@PathVariable id: Short): ResponseEntity<DeviceCategoryResponse> {
        val category = deviceCategoryService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(category)
    }

    @PostMapping
    @Operation(summary = "Crear nueva categoría de dispositivo")
    @ApiResponses(ApiResponse(responseCode = "201"), ApiResponse(responseCode = "400"))
    fun create(@Valid @RequestBody request: DeviceCategoryCreateRequest): ResponseEntity<DeviceCategoryResponse> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(deviceCategoryService.create(request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría de dispositivo")
    @ApiResponses(ApiResponse(responseCode = "200"), ApiResponse(responseCode = "400"), ApiResponse(responseCode = "404"))
    fun update(@PathVariable id: Short, @Valid @RequestBody request: DeviceCategoryUpdateRequest): ResponseEntity<DeviceCategoryResponse> {
        return try {
            val updated = deviceCategoryService.update(id, request) ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría de dispositivo")
    @ApiResponses(ApiResponse(responseCode = "204"), ApiResponse(responseCode = "404"), ApiResponse(responseCode = "409"))
    fun delete(@PathVariable id: Short): ResponseEntity<Void> {
        return try {
            if (deviceCategoryService.delete(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}
