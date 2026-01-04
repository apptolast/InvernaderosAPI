package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.alert.dto.AlertCreateRequest
import com.apptolast.invernaderos.features.alert.dto.AlertResolveRequest
import com.apptolast.invernaderos.features.alert.dto.AlertResponse
import com.apptolast.invernaderos.features.alert.dto.AlertUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/alerts")
@Tag(name = "Tenant Alert Management", description = "Endpoints para la gestión de alertas de un cliente")
class TenantAlertController(
    private val alertService: AlertService
) {

    @GetMapping
    @Operation(summary = "Obtener todas las alertas de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: UUID): ResponseEntity<List<AlertResponse>> {
        return ResponseEntity.ok(alertService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Obtener una alerta específica de un cliente")
    fun getById(
        @PathVariable tenantId: UUID,
        @PathVariable alertId: UUID
    ): ResponseEntity<AlertResponse> {
        val alert = alertService.findByIdAndTenantId(alertId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(alert)
    }

    @PostMapping
    @Operation(summary = "Crear una nueva alerta para un cliente")
    fun create(
        @PathVariable tenantId: UUID,
        @RequestBody request: AlertCreateRequest
    ): ResponseEntity<AlertResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createForTenant(tenantId, request))
    }

    @PutMapping("/{alertId}")
    @Operation(summary = "Actualizar una alerta existente de un cliente")
    fun update(
        @PathVariable tenantId: UUID,
        @PathVariable alertId: UUID,
        @RequestBody request: AlertUpdateRequest
    ): ResponseEntity<AlertResponse> {
        val updated = alertService.updateForTenant(alertId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{alertId}")
    @Operation(summary = "Eliminar una alerta de un cliente")
    fun delete(
        @PathVariable tenantId: UUID,
        @PathVariable alertId: UUID
    ): ResponseEntity<Unit> {
        return if (alertService.deleteForTenant(alertId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolver una alerta")
    fun resolve(
        @PathVariable tenantId: UUID,
        @PathVariable alertId: UUID,
        @RequestBody(required = false) request: AlertResolveRequest?
    ): ResponseEntity<AlertResponse> {
        val resolved = alertService.resolveForTenant(alertId, tenantId, request?.resolvedByUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(resolved)
    }

    @PostMapping("/{alertId}/reopen")
    @Operation(summary = "Reabrir una alerta resuelta")
    fun reopen(
        @PathVariable tenantId: UUID,
        @PathVariable alertId: UUID
    ): ResponseEntity<AlertResponse> {
        val reopened = alertService.reopenForTenant(alertId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(reopened)
    }
}
