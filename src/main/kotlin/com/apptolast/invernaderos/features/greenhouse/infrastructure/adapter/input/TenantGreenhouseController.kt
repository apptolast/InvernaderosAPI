package com.apptolast.invernaderos.features.greenhouse.infrastructure.adapter.input

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.DeleteGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.FindGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.dto.mapper.toCommand
import com.apptolast.invernaderos.features.greenhouse.dto.mapper.toResponse
import com.apptolast.invernaderos.features.greenhouse.dto.request.GreenhouseCreateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.request.GreenhouseUpdateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.response.GreenhouseResponse
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/greenhouses")
@Tag(name = "Tenant Greenhouse Management", description = "Endpoints para la gestión de invernaderos de un cliente")
class TenantGreenhouseController(
    private val createUseCase: CreateGreenhouseUseCase,
    private val findUseCase: FindGreenhouseUseCase,
    private val updateUseCase: UpdateGreenhouseUseCase,
    private val deleteUseCase: DeleteGreenhouseUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los invernaderos de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<GreenhouseResponse>> {
        val greenhouses = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(greenhouses.map { it.toResponse() })
    }

    @GetMapping("/{greenhouseId}")
    @Operation(summary = "Obtener un invernadero específico de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long
    ): ResponseEntity<GreenhouseResponse> {
        return findUseCase.findById(GreenhouseId(greenhouseId), TenantId(tenantId)).fold(
            onLeft = { error ->
                ResponseEntity.notFound().build()
            },
            onRight = { greenhouse ->
                ResponseEntity.ok(greenhouse.toResponse())
            }
        )
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo invernadero para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: GreenhouseCreateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(TenantId(tenantId))
        return createUseCase.execute(command).fold(
            onLeft = { error ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
            },
            onRight = { greenhouse ->
                ResponseEntity.status(HttpStatus.CREATED).body(greenhouse.toResponse())
            }
        )
    }

    @PutMapping("/{greenhouseId}")
    @Operation(summary = "Actualizar un invernadero existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long,
        @RequestBody request: GreenhouseUpdateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(GreenhouseId(greenhouseId), TenantId(tenantId))
        return updateUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError.NotFound ->
                        ResponseEntity.notFound().build()
                    is com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError.DuplicateName ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                }
            },
            onRight = { greenhouse ->
                ResponseEntity.ok(greenhouse.toResponse())
            }
        )
    }

    @DeleteMapping("/{greenhouseId}")
    @Operation(summary = "Eliminar un invernadero de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable greenhouseId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(GreenhouseId(greenhouseId), TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }
}
