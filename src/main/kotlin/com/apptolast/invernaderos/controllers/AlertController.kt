package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.entity.Alert
import com.apptolast.invernaderos.service.AlertService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

/**
 * REST Controller para gestión de Alertas.
 *
 * Endpoints disponibles:
 * - GET /api/alerts - Todas las alertas (con filtros opcionales)
 * - GET /api/alerts/{id} - Alerta por ID
 * - GET /api/alerts/tenant/{tenantId} - Alertas por tenant
 * - GET /api/alerts/greenhouse/{greenhouseId} - Alertas por greenhouse
 * - GET /api/alerts/sensor/{sensorId} - Alertas por sensor
 * - GET /api/alerts/actuator/{actuatorId} - Alertas por actuador
 * - GET /api/alerts/unresolved/tenant/{tenantId} - Alertas no resueltas por tenant
 * - GET /api/alerts/unresolved/greenhouse/{greenhouseId} - Alertas no resueltas por greenhouse
 * - GET /api/alerts/count/unresolved/tenant/{tenantId} - Cuenta alertas no resueltas
 * - GET /api/alerts/count/critical/tenant/{tenantId} - Cuenta alertas críticas
 * - POST /api/alerts - Crear nueva alerta
 * - PUT /api/alerts/{id} - Actualizar alerta
 * - PUT /api/alerts/{id}/resolve - Resolver alerta
 * - PUT /api/alerts/{id}/reopen - Reabrir alerta
 * - DELETE /api/alerts/{id} - Eliminar alerta
 *
 * Best Practices Applied:
 * - Bean Validation with @Valid for request bodies
 * - Proper HTTP status codes (200 OK, 201 CREATED, 404 NOT FOUND, 400 BAD REQUEST)
 * - Exception handling for resilience
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html">Spring MVC Validation</a>
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = ["*"]) // TODO: Restrict to specific origins in production
@Validated
class AlertController(
    private val alertService: AlertService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/alerts
     *
     * Obtiene alertas con filtros opcionales.
     *
     * Query params:
     * - tenantId: Filtrar por tenant (requerido para queries multi-tenant)
     * - greenhouseId: Filtrar por greenhouse (opcional)
     * - severity: Filtrar por severidad (INFO, WARNING, ERROR, CRITICAL)
     * - isResolved: Filtrar por estado (true/false)
     * - limit: Limitar número de resultados
     *
     * Response: List<Alert>
     */
    @GetMapping
    fun getAlerts(
        @RequestParam tenantId: UUID,
        @RequestParam(required = false) greenhouseId: UUID?,
        @RequestParam(required = false) severity: String?,
        @RequestParam(required = false) isResolved: Boolean?,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts?tenantId=$tenantId&greenhouseId=$greenhouseId&severity=$severity&isResolved=$isResolved&limit=$limit")

        return try {
            val alerts = when {
                greenhouseId != null || severity != null || isResolved != null -> {
                    // Usar filtros combinados
                    alertService.getByFilters(tenantId, greenhouseId, severity, isResolved)
                }
                else -> {
                    // Solo tenant
                    alertService.getAllByTenant(tenantId)
                }
            }.take(limit)

            ResponseEntity.ok(alerts)
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
    fun getAlertById(@PathVariable id: UUID): ResponseEntity<Alert> {
        logger.debug("GET /api/alerts/$id")

        val alert = alertService.getById(id)
        return if (alert != null) {
            ResponseEntity.ok(alert)
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
        @PathVariable tenantId: UUID,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/tenant/$tenantId?limit=$limit")

        return try {
            val alerts = alertService.getAllByTenant(tenantId).take(limit)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/greenhouse/{greenhouseId}
     *
     * Obtiene todas las alertas de un greenhouse.
     */
    @GetMapping("/greenhouse/{greenhouseId}")
    fun getAlertsByGreenhouse(@PathVariable greenhouseId: UUID): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/greenhouse/$greenhouseId")

        return try {
            val alerts = alertService.getAllByGreenhouse(greenhouseId)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts for greenhouse: $greenhouseId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/sensor/{sensorId}
     *
     * Obtiene alertas relacionadas con un sensor.
     */
    @GetMapping("/sensor/{sensorId}")
    fun getAlertsBySensor(@PathVariable sensorId: UUID): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/sensor/$sensorId")

        return try {
            val alerts = alertService.getBySensor(sensorId)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting alerts for sensor: $sensorId", e)
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
    fun getUnresolvedByTenant(@PathVariable tenantId: UUID): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/unresolved/tenant/$tenantId")

        return try {
            val alerts = alertService.getUnresolvedByTenantOrderedBySeverity(tenantId)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting unresolved alerts for tenant: $tenantId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/alerts/unresolved/greenhouse/{greenhouseId}
     *
     * Obtiene alertas no resueltas por greenhouse, ordenadas por severidad.
     */
    @GetMapping("/unresolved/greenhouse/{greenhouseId}")
    fun getUnresolvedByGreenhouse(@PathVariable greenhouseId: UUID): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/unresolved/greenhouse/$greenhouseId")

        return try {
            val alerts = alertService.getUnresolvedByGreenhouseOrderedBySeverity(greenhouseId)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting unresolved alerts for greenhouse: $greenhouseId", e)
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
    fun countUnresolvedByTenant(@PathVariable tenantId: UUID): ResponseEntity<Map<String, Long>> {
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
    fun countCriticalByTenant(@PathVariable tenantId: UUID): ResponseEntity<Map<String, Long>> {
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
        @PathVariable tenantId: UUID,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<List<Alert>> {
        logger.debug("GET /api/alerts/recent/tenant/$tenantId?limit=$limit")

        return try {
            val alerts = alertService.getRecentByTenant(tenantId, limit)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            logger.error("Error getting recent alerts for tenant: $tenantId", e)
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
    fun createAlert(@Valid @RequestBody alert: Alert): ResponseEntity<Alert> {
        logger.debug("POST /api/alerts - Creating alert: ${alert.alertType}")

        return try {
            val created = alertService.create(alert)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
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
    fun updateAlert(@PathVariable id: UUID, @Valid @RequestBody alert: Alert): ResponseEntity<Alert> {
        logger.debug("PUT /api/alerts/$id - Updating alert")

        return try {
            val updated = alertService.update(id, alert)
            if (updated != null) {
                ResponseEntity.ok(updated)
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
     * Resuelve una alerta.
     *
     * Query params:
     * - userId: UUID del usuario que resuelve (opcional)
     * - userName: Nombre del usuario que resuelve (opcional)
     *
     * Response: Alert resuelto
     */
    @PutMapping("/{id}/resolve")
    fun resolveAlert(
        @PathVariable id: UUID,
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) userName: String?
    ): ResponseEntity<Alert> {
        logger.debug("PUT /api/alerts/$id/resolve - Resolving alert")

        return try {
            val resolved = alertService.resolve(id, userId, userName)
            if (resolved != null) {
                ResponseEntity.ok(resolved)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error resolving alert: $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * PUT /api/alerts/{id}/reopen
     *
     * Reabre una alerta resuelta.
     *
     * Response: Alert reabierto
     */
    @PutMapping("/{id}/reopen")
    fun reopenAlert(@PathVariable id: UUID): ResponseEntity<Alert> {
        logger.debug("PUT /api/alerts/$id/reopen - Reopening alert")

        return try {
            val reopened = alertService.reopen(id)
            if (reopened != null) {
                ResponseEntity.ok(reopened)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error reopening alert: $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * DELETE /api/alerts/{id}
     *
     * Elimina una alerta.
     *
     * Response: 204 No Content si se eliminó, 404 si no existe
     */
    @DeleteMapping("/{id}")
    fun deleteAlert(@PathVariable id: UUID): ResponseEntity<Void> {
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
