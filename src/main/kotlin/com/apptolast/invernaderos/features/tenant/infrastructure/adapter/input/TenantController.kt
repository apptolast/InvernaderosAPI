package com.apptolast.invernaderos.features.tenant.infrastructure.adapter.input

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.DeleteTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.FindTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.TenantFilter
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantUseCase
import com.apptolast.invernaderos.features.tenant.dto.mapper.toCommand
import com.apptolast.invernaderos.features.tenant.dto.mapper.toResponse
import com.apptolast.invernaderos.features.tenant.dto.mapper.toTenantStatus
import com.apptolast.invernaderos.features.tenant.dto.request.TenantCreateRequest
import com.apptolast.invernaderos.features.tenant.dto.request.TenantUpdateRequest
import com.apptolast.invernaderos.features.tenant.dto.response.TenantResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Management", description = "Endpoints para el CRUD de Tenants (Clientes)")
class TenantController(
    private val createUseCase: CreateTenantUseCase,
    private val findUseCase: FindTenantUseCase,
    private val updateUseCase: UpdateTenantUseCase,
    private val deleteUseCase: DeleteTenantUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los tenants con filtros opcionales")
    fun getAllTenants(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) province: String?,
        @RequestParam(required = false) isActive: Boolean?
    ): ResponseEntity<List<TenantResponse>> {
        val filter = TenantFilter(
            search = search,
            province = province,
            status = isActive?.toTenantStatus()
        )
        val tenants = findUseCase.findAll(filter)
        return ResponseEntity.ok(tenants.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tenant por ID")
    fun getTenantById(@PathVariable id: Long): ResponseEntity<TenantResponse> {
        return findUseCase.findById(TenantId(id)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo tenant")
    fun createTenant(@RequestBody request: TenantCreateRequest): ResponseEntity<Any> {
        return createUseCase.execute(request.toCommand()).fold(
            onLeft = { error ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
            },
            onRight = { tenant ->
                ResponseEntity.status(HttpStatus.CREATED).body(tenant.toResponse())
            }
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un tenant existente")
    fun updateTenant(
        @PathVariable id: Long,
        @RequestBody request: TenantUpdateRequest
    ): ResponseEntity<Any> {
        return updateUseCase.execute(request.toCommand(TenantId(id))).fold(
            onLeft = { error ->
                when (error) {
                    is TenantError.NotFound -> ResponseEntity.notFound().build()
                    is TenantError.DuplicateName ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is TenantError.DuplicateEmail ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                }
            },
            onRight = { tenant ->
                ResponseEntity.ok(tenant.toResponse())
            }
        )
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un tenant")
    fun deleteTenant(@PathVariable id: Long): ResponseEntity<Unit> {
        return deleteUseCase.execute(TenantId(id)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }
}
