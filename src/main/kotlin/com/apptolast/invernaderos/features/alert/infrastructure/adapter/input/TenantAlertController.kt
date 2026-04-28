package com.apptolast.invernaderos.features.alert.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.DeleteAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertUseCase
import com.apptolast.invernaderos.features.alert.dto.mapper.toCommand
import com.apptolast.invernaderos.features.alert.dto.mapper.toResponse
import com.apptolast.invernaderos.features.alert.dto.request.AlertCreateRequest
import com.apptolast.invernaderos.features.alert.dto.request.AlertResolveRequest
import com.apptolast.invernaderos.features.alert.dto.request.AlertUpdateRequest
import com.apptolast.invernaderos.features.alert.dto.response.AlertResponse
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/alerts")
@Tag(name = "Tenant Alert Management", description = "Endpoints para la gestion de alertas de un cliente")
class TenantAlertController(
    private val createUseCase: CreateAlertUseCase,
    private val findUseCase: FindAlertUseCase,
    private val updateUseCase: UpdateAlertUseCase,
    private val deleteUseCase: DeleteAlertUseCase,
    private val restInboundAdapter: AlertRestInboundAdapter
) {

    @GetMapping
    @Operation(summary = "Obtener todas las alertas de un cliente")
    fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<List<AlertResponse>> {
        val alerts = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(alerts.map { it.toResponse() })
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Obtener una alerta especifica de un cliente")
    fun getById(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long
    ): ResponseEntity<AlertResponse> {
        return findUseCase.findByIdAndTenantId(alertId, TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear una nueva alerta para un cliente")
    fun create(
        @PathVariable tenantId: Long,
        @RequestBody request: AlertCreateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(TenantId(tenantId))
        return createUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is AlertError.NotFound ->
                        ResponseEntity.notFound().build()
                    is AlertError.AlreadyResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    else ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                }
            },
            onRight = { alert ->
                ResponseEntity.status(HttpStatus.CREATED).body(alert.toResponse())
            }
        )
    }

    @PutMapping("/{alertId}")
    @Operation(summary = "Actualizar una alerta existente de un cliente")
    fun update(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long,
        @RequestBody request: AlertUpdateRequest
    ): ResponseEntity<Any> {
        val command = request.toCommand(alertId, TenantId(tenantId))
        return updateUseCase.execute(command).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.NotFound ->
                        ResponseEntity.notFound().build()
                    is AlertError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    is AlertError.AlreadyResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    else ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                }
            },
            onRight = { alert ->
                ResponseEntity.ok(alert.toResponse())
            }
        )
    }

    @DeleteMapping("/{alertId}")
    @Operation(summary = "Eliminar una alerta de un cliente")
    fun delete(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(alertId, TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolver una alerta")
    fun resolve(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long,
        @RequestBody(required = false) request: AlertResolveRequest?
    ): ResponseEntity<Any> {
        return restInboundAdapter.resolve(alertId, TenantId(tenantId), request?.resolvedByUserId).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.NotFound ->
                        ResponseEntity.notFound().build()
                    is AlertError.AlreadyResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is AlertError.NotResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is AlertError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    else ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                }
            },
            onRight = { alert ->
                ResponseEntity.ok(alert.toResponse())
            }
        )
    }

    @PostMapping("/{alertId}/reopen")
    @Operation(summary = "Reabrir una alerta resuelta")
    fun reopen(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long
    ): ResponseEntity<Any> {
        return restInboundAdapter.reopen(alertId, TenantId(tenantId)).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.NotFound ->
                        ResponseEntity.notFound().build()
                    is AlertError.AlreadyResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is AlertError.NotResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is AlertError.SectorNotOwnedByTenant ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
                    else ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                }
            },
            onRight = { alert ->
                ResponseEntity.ok(alert.toResponse())
            }
        )
    }
}
