package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.alert.Alert
import com.apptolast.invernaderos.features.alert.AlertService
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.dto.mapper.toResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertResponse
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertRestInboundAdapter
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * REST Controller para gestión de Alertas.
 *
 * Endpoints disponibles:
 * - GET /api/v1/alerts - Todas las alertas (con filtros opcionales)
 * - GET /api/v1/alerts/{id} - Alerta por ID
 * - GET /api/v1/alerts/tenant/{tenantId} - Alertas por tenant
 * - GET /api/v1/alerts/sector/{sectorId} - Alertas por sector
 * - GET /api/v1/alerts/sensor/{sensorId} - Alertas por sensor
 * - GET /api/v1/alerts/actuator/{actuatorId} - Alertas por actuador
 * - GET /api/v1/alerts/unresolved/tenant/{tenantId} - Alertas no resueltas por tenant
 * - GET /api/v1/alerts/unresolved/sector/{sectorId} - Alertas no resueltas por sector
 * - GET /api/v1/alerts/history/tenant/{tenantId} - Histórico completo (activas + resueltas) de un tenant
 * - GET /api/v1/alerts/count/unresolved/tenant/{tenantId} - Cuenta alertas no resueltas
 * - GET /api/v1/alerts/count/critical/tenant/{tenantId} - Cuenta alertas críticas
 * - POST /api/v1/alerts - Crear nueva alerta
 * - PUT /api/v1/alerts/{id} - Actualizar alerta
 * - PUT /api/v1/alerts/{id}/resolve - Resolver alerta
 * - PUT /api/v1/alerts/{id}/reopen - Reabrir alerta
 * - DELETE /api/v1/alerts/{id} - Eliminar alerta
 *
 * Best Practices Applied:
 * - Bean Validation with @Valid for request bodies
 * - Proper HTTP status codes (200 OK, 201 CREATED, 404 NOT FOUND, 400 BAD REQUEST)
 * - Exception handling for resilience
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html">Spring MVC Validation</a>
 */
@RestController
@RequestMapping("/api/v1/alerts")
@CrossOrigin(origins = ["*"]) // TODO: Restrict to specific origins in production
@Validated
class AlertController(
    private val alertService: AlertService,
    private val restInboundAdapter: AlertRestInboundAdapter,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * Sunset date is 90 days after application startup. Computed once at class load time
         * so the value is stable across requests and cheap to produce.
         */
        private val SUNSET_DATE: String = ZonedDateTime.now(ZoneOffset.UTC)
            .plusDays(90)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME)

        private fun deprecationHeaders(): HttpHeaders = HttpHeaders().apply {
            set("Deprecation", "true")
            set("Sunset", SUNSET_DATE)
            set(
                "Link",
                """<https://inverapi-prod.apptolast.com/swagger-ui.html#tenant-alerts>; rel="successor-version""""
            )
        }
    }

    /**
     * GET /api/alerts
     *
     * Obtiene alertas con filtros opcionales.
     *
     * Query params:
     * - tenantId: Filtrar por tenant (requerido para queries multi-tenant)
     * - sectorId: Filtrar por sector (opcional)
     * - severity: Filtrar por severidad (INFO, WARNING, ERROR, CRITICAL)
     * - isResolved: Filtrar por estado (true/false)
     * - limit: Limitar número de resultados
     *
     * Response: List<Alert>
     */
    @GetMapping
    fun getAlerts(
        @RequestParam tenantId: Long,
        @RequestParam(required = false) sectorId: Long?,
        @RequestParam(required = false) severity: String?,
        @RequestParam(required = false) isResolved: Boolean?,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts?tenantId=$tenantId&sectorId=$sectorId&severity=$severity&isResolved=$isResolved&limit=$limit")

        return try {
            val alerts = when {
                sectorId != null || severity != null || isResolved != null -> {
                    // Usar filtros combinados
                    alertService.getByFilters(tenantId, sectorId, severity, isResolved)
                }
                else -> {
                    // Solo tenant
                    alertService.getAllByTenant(tenantId)
                }
            }.take(limit).map { it.toResponse() }

            ResponseEntity.ok().headers(deprecationHeaders()).body(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/{id}
     *
     * Obtiene una alerta por ID.
     */
    @GetMapping("/{id}")
    fun getAlertById(@PathVariable id: Long): ResponseEntity<AlertResponse> {
        logger.debug("GET /api/alerts/$id")

        val alert = alertService.getById(id)
        return if (alert != null) {
            ResponseEntity.ok(alert.toResponse())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * GET /api/alerts/tenant/{tenantId}
     *
     * Obtiene todas las alertas de un tenant.
     *
     * Query params:
     * - limit: Número máximo de resultados (default: 100)
     */
    @GetMapping("/tenant/{tenantId}")
    fun getAlertsByTenant(
        @PathVariable tenantId: Long,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/tenant/$tenantId?limit=$limit")

        return try {
            val alerts = alertService.getAllByTenant(tenantId).take(limit).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/sector/{sectorId}
     *
     * Obtiene todas las alertas de un sector.
     */
    @GetMapping("/sector/{sectorId}")
    fun getAlertsBySector(@PathVariable sectorId: Long): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/sector/$sectorId")

        return try {
            val alerts = alertService.getAllBySector(sectorId).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts for sector: $sectorId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/unresolved/tenant/{tenantId}
     *
     * Obtiene alertas no resueltas por tenant, ordenadas por severidad.
     * CRITICAL primero, luego ERROR, WARNING, INFO.
     */
    @GetMapping("/unresolved/tenant/{tenantId}")
    fun getUnresolvedByTenant(@PathVariable tenantId: Long): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/unresolved/tenant/$tenantId")

        return try {
            val alerts = alertService.getUnresolvedByTenantOrderedBySeverity(tenantId).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting unresolved alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/unresolved/sector/{sectorId}
     *
     * Obtiene alertas no resueltas por sector, ordenadas por severidad.
     */
    @GetMapping("/unresolved/sector/{sectorId}")
    fun getUnresolvedBySector(@PathVariable sectorId: Long): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/unresolved/sector/$sectorId")

        return try {
            val alerts = alertService.getUnresolvedBySectorOrderedBySeverity(sectorId).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting unresolved alerts for sector: $sectorId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/count/unresolved/tenant/{tenantId}
     *
     * Cuenta alertas no resueltas por tenant.
     *
     * Response: { "count": 42 }
     */
    @GetMapping("/count/unresolved/tenant/{tenantId}")
    fun countUnresolvedByTenant(@PathVariable tenantId: Long): ResponseEntity<Map<String, Long>> {
        logger.debug("GET /api/alerts/count/unresolved/tenant/$tenantId")

        return try {
            val count = alertService.countUnresolvedByTenant(tenantId)
            ResponseEntity.ok(mapOf("count" to count))
        } catch (e: Exception) {
            logger.error("Error counting unresolved alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/count/critical/tenant/{tenantId}
     *
     * Cuenta alertas críticas no resueltas por tenant.
     *
     * Response: { "count": 5 }
     */
    @GetMapping("/count/critical/tenant/{tenantId}")
    fun countCriticalByTenant(@PathVariable tenantId: Long): ResponseEntity<Map<String, Long>> {
        logger.debug("GET /api/alerts/count/critical/tenant/$tenantId")

        return try {
            val count = alertService.countCriticalUnresolvedByTenant(tenantId)
            ResponseEntity.ok(mapOf("count" to count))
        } catch (e: Exception) {
            logger.error("Error counting critical alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/recent/tenant/{tenantId}
     *
     * Obtiene las últimas N alertas de un tenant.
     *
     * Query params:
     * - limit: Número de alertas (default: 50)
     */
    @GetMapping("/recent/tenant/{tenantId}")
    fun getRecentByTenant(
        @PathVariable tenantId: Long,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/recent/tenant/$tenantId?limit=$limit")

        return try {
            val alerts = alertService.getRecentByTenant(tenantId, limit).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting recent alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/history/tenant/{tenantId}
     *
     * Histórico completo de alertas del tenant: incluye tanto activas como resueltas,
     * ordenadas por createdAt DESC. Es lo que la app móvil consume en su pantalla
     * "Histórico" — a diferencia del filtro `?isResolved=true`, este endpoint no
     * limita por estado de resolución.
     *
     * Query params:
     * - limit: Número de alertas (default: 100)
     */
    @GetMapping("/history/tenant/{tenantId}")
    fun getHistoryByTenant(
        @PathVariable tenantId: Long,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<AlertResponse>> {
        logger.debug("GET /api/alerts/history/tenant/$tenantId?limit=$limit")

        return try {
            val alerts = alertService.getHistoryByTenant(tenantId, limit).map { it.toResponse() }
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alert history for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * POST /api/alerts
     *
     * Crea una nueva alerta.
     *
     * Request body: Alert (JSON)
     * Response: Alert creado con ID generado
     *
     * Bean Validation ensures:
     * - Required fields are not null
     * - Field constraints are satisfied
     */
    @PostMapping
    fun createAlert(@Valid @RequestBody alert: Alert): ResponseEntity<AlertResponse> {
        logger.debug("POST /api/alerts - Creating alert: ${alert.alertType}")

        return try {
            val created = alertService.create(alert)
            // Re-fetch with EntityGraph so the response includes sectorCode, severityName, etc.
            val hydrated = created.id?.let { alertService.getById(it) } ?: created
            ResponseEntity.status(HttpStatus.CREATED).body(hydrated.toResponse())
        } catch (e: Exception) {
            logger.error("Error creating alert", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * PUT /api/alerts/{id}
     *
     * Actualiza una alerta existente.
     *
     * Request body: Alert (JSON)
     * Response: Alert actualizado
     */
    @PutMapping("/{id}")
    fun updateAlert(@PathVariable id: Long, @Valid @RequestBody alert: Alert): ResponseEntity<AlertResponse> {
        logger.debug("PUT /api/alerts/$id - Updating alert")

        return try {
            val updated = alertService.update(id, alert)
            if (updated != null) {
                val hydrated = alertService.getById(id) ?: updated
                ResponseEntity.ok(hydrated.toResponse())
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error updating alert: $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * PUT /api/alerts/{id}/resolve
     *
     * Resuelve una alerta. Delegated to hexagonal use case so that an audit row is written
     * to alert_state_changes and the AlertStateChangedEvent is published.
     *
     * Query params:
     * - userId: ID del usuario que resuelve (opcional)
     * - userName: Nombre del usuario (ignorado, kept for backward compat)
     *
     * Response: Alert resuelto
     *
     * @deprecated Use POST /api/v1/tenants/{tenantId}/alerts/{alertId}/resolve
     */
    @PutMapping("/{id}/resolve")
    fun resolveAlert(
        @PathVariable id: Long,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) userName: String?
    ): ResponseEntity<AlertResponse> {
        logger.debug("PUT /api/alerts/$id/resolve - Resolving alert (legacy)")

        val alert = alertService.getById(id) ?: return ResponseEntity.notFound().build()
        val tenantId = TenantId(alert.tenantId)

        return restInboundAdapter.resolve(id, tenantId, userId).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.NotFound -> ResponseEntity.notFound().build()
                    is AlertError.AlreadyResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).headers(deprecationHeaders()).build()
                    else ->
                        ResponseEntity.internalServerError().build()
                }
            },
            onRight = { resolved ->
                // Re-fetch with EntityGraph so the response includes joined fields (sectorCode, etc.)
                val hydratedAlert = alertService.getById(id)
                val response = hydratedAlert?.toResponse() ?: resolved.toResponse()
                ResponseEntity.ok().headers(deprecationHeaders()).body(response)
            }
        )
    }

    /**
     * PUT /api/alerts/{id}/reopen
     *
     * Reabre una alerta resuelta. Delegated to hexagonal use case so that an audit row is
     * written to alert_state_changes and the AlertStateChangedEvent is published.
     *
     * Response: Alert reabierto
     *
     * @deprecated Use POST /api/v1/tenants/{tenantId}/alerts/{alertId}/reopen
     */
    @PutMapping("/{id}/reopen")
    fun reopenAlert(@PathVariable id: Long): ResponseEntity<AlertResponse> {
        logger.debug("PUT /api/alerts/$id/reopen - Reopening alert (legacy)")

        val alert = alertService.getById(id) ?: return ResponseEntity.notFound().build()
        val tenantId = TenantId(alert.tenantId)

        return restInboundAdapter.reopen(id, tenantId, actorUserId = null).fold(
            onLeft = { error ->
                when (error) {
                    is AlertError.NotFound -> ResponseEntity.notFound().build()
                    is AlertError.NotResolved ->
                        ResponseEntity.status(HttpStatus.CONFLICT).headers(deprecationHeaders()).build()
                    else ->
                        ResponseEntity.internalServerError().build()
                }
            },
            onRight = { reopened ->
                // Re-fetch with EntityGraph so the response includes joined fields (sectorCode, etc.)
                val hydratedAlert = alertService.getById(id)
                val response = hydratedAlert?.toResponse() ?: reopened.toResponse()
                ResponseEntity.ok().headers(deprecationHeaders()).body(response)
            }
        )
    }

    /**
     * DELETE /api/alerts/{id}
     *
     * Elimina una alerta.
     *
     * Response: 204 No Content si se eliminó, 404 si no existe
     */
    @DeleteMapping("/{id}")
    fun deleteAlert(@PathVariable id: Long): ResponseEntity<Void> {
        logger.debug("DELETE /api/alerts/$id - Deleting alert")

        return try {
            val deleted = alertService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error deleting alert: $id", e)
            ResponseEntity.internalServerError().build()
        }
    }
}
