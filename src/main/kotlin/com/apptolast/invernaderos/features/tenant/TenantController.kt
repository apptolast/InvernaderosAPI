package com.apptolast.invernaderos.features.tenant

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Management", description = "Endpoints para el CRUD de Tenants (Clientes)")
class TenantController(
    private val tenantService: TenantService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los tenants con filtros opcionales")
    fun getAllTenants(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) province: String?,
        @RequestParam(required = false) isActive: Boolean?
    ): ResponseEntity<List<TenantResponse>> {
        return ResponseEntity.ok(tenantService.findAll(search, province, isActive))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tenant por ID")
    fun getTenantById(@PathVariable id: Long): ResponseEntity<TenantResponse> {
        val tenant = tenantService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tenant)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo tenant")
    fun createTenant(@RequestBody request: TenantCreateRequest): ResponseEntity<TenantResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un tenant existente")
    fun updateTenant(
        @PathVariable id: Long,
        @RequestBody request: TenantUpdateRequest
    ): ResponseEntity<TenantResponse> {
        val updated = tenantService.update(id, request) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un tenant")
    fun deleteTenant(@PathVariable id: Long): ResponseEntity<Unit> {
        return if (tenantService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
