package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.alert.dto.AlertCreateRequest
import com.apptolast.invernaderos.features.alert.dto.AlertResponse
import com.apptolast.invernaderos.features.alert.dto.AlertUpdateRequest
import com.apptolast.invernaderos.features.alert.dto.toResponse
import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para gestión de Alertas.
 *
 * Maneja la lógica de negocio para:
 * - Crear, actualizar, resolver alertas
 * - Buscar alertas por tenant, greenhouse, sensor, actuador
 * - Filtrar por severidad, tipo, estado
 * - Queries optimizados multi-tenant
 *
 * Best Practices Applied:
 * - Method-level @Transactional for granular control
 * - readOnly=true for query operations (performance optimization)
 * - Explicit rollbackFor for write operations
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html">Spring @Transactional Documentation</a>
 */
@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val greenhouseRepository: GreenhouseRepository,
    private val codeGeneratorService: CodeGeneratorService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todas las alertas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenant(tenantId: Long): List<Alert> {
        logger.debug("Getting all alerts for tenant: $tenantId")
        return alertRepository.findByTenantId(tenantId)
    }

    /**
     * Obtiene alertas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByGreenhouse(greenhouseId: Long): List<Alert> {
        logger.debug("Getting all alerts for greenhouse: $greenhouseId")
        return alertRepository.findByGreenhouseId(greenhouseId)
    }

    /**
     * Obtiene alertas por tenant y greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenantAndGreenhouse(tenantId: Long, greenhouseId: Long): List<Alert> {
        logger.debug("Getting alerts for tenant: $tenantId, greenhouse: $greenhouseId")
        return alertRepository.findByTenantIdAndGreenhouseId(tenantId, greenhouseId)
    }

    /**
     * Obtiene alertas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByTenant(tenantId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts for tenant: $tenantId")
        return alertRepository.findByTenantIdAndIsResolvedFalse(tenantId)
    }

    /**
     * Obtiene alertas no resueltas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByGreenhouse(greenhouseId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts for greenhouse: $greenhouseId")
        return alertRepository.findByGreenhouseIdAndIsResolvedFalse(greenhouseId)
    }

    /**
     * Obtiene alertas no resueltas ordenadas por severidad
     * CRITICAL primero, luego ERROR, WARNING, INFO
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByTenantOrderedBySeverity(tenantId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts ordered by severity for tenant: $tenantId")
        return alertRepository.findUnresolvedByTenantOrderedBySeverity(tenantId)
    }

    /**
     * Obtiene alertas no resueltas por greenhouse ordenadas por severidad
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByGreenhouseOrderedBySeverity(greenhouseId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts ordered by severity for greenhouse: $greenhouseId")
        return alertRepository.findUnresolvedByGreenhouseOrderedBySeverity(greenhouseId)
    }

    /**
     * Busca alertas en rango de fechas
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getByDateRange(startDate: Instant, endDate: Instant): List<Alert> {
        logger.debug("Getting alerts between $startDate and $endDate")
        return alertRepository.findByCreatedAtBetween(startDate, endDate)
    }

    /**
     * Busca alertas con filtros combinados
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getByFilters(
        tenantId: Long,
        greenhouseId: Long?,
        severity: String?,
        isResolved: Boolean?
    ): List<Alert> {
        logger.debug("Getting alerts with filters - tenant: $tenantId, greenhouse: $greenhouseId, severity: $severity, isResolved: $isResolved")
        return alertRepository.findByFilters(tenantId, greenhouseId, severity, isResolved)
    }

    /**
     * Cuenta alertas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countUnresolvedByTenant(tenantId: Long): Long {
        return alertRepository.countByTenantIdAndIsResolvedFalse(tenantId)
    }

    /**
     * Cuenta alertas no resueltas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countUnresolvedByGreenhouse(greenhouseId: Long): Long {
        return alertRepository.countByGreenhouseIdAndIsResolvedFalse(greenhouseId)
    }

    /**
     * Cuenta alertas críticas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countCriticalUnresolvedByTenant(tenantId: Long): Long {
        return alertRepository.countCriticalUnresolvedByTenant(tenantId)
    }

    /**
     * Obtiene las últimas N alertas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getRecentByTenant(tenantId: Long, limit: Int = 50): List<Alert> {
        logger.debug("Getting recent $limit alerts for tenant: $tenantId")
        return alertRepository.findRecentByTenant(tenantId, limit)
    }

    /**
     * Busca una alerta por ID
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getById(id: Long): Alert? {
        logger.debug("Getting alert by ID: $id")
        return alertRepository.findById(id).orElse(null)
    }

    /**
     * Crea una nueva alerta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun create(alert: Alert): Alert {
        logger.info("Creating new alert: type=${alert.alertTypeId}, severity=${alert.severityId}, greenhouse=${alert.greenhouseId}")
        return alertRepository.save(alert)
    }

    /**
     * Actualiza una alerta existente
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun update(id: Long, alert: Alert): Alert? {
        if (!alertRepository.existsById(id)) {
            logger.warn("Alert not found for update: ID=$id")
            return null
        }

        val updated = alert.copy(id = id, updatedAt = Instant.now())
        logger.info("Updating alert: ID=$id")
        return alertRepository.save(updated)
    }

    /**
     * Resuelve una alerta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun resolve(id: Long, resolvedByUserId: Long?, resolvedBy: String? = null): Alert? {
        val alert = alertRepository.findById(id).orElse(null)
        if (alert == null) {
            logger.warn("Alert not found for resolution: ID=$id")
            return null
        }

        if (alert.isResolved) {
            logger.warn("Alert already resolved: ID=$id")
            return alert
        }

        val resolved = alert.copy(
            isResolved = true,
            resolvedAt = Instant.now(),
            resolvedByUserId = resolvedByUserId,
            updatedAt = Instant.now()
        )

        logger.info("Resolving alert: ID=$id, resolvedByUserId=$resolvedByUserId")
        return alertRepository.save(resolved)
    }

    /**
     * Reabre una alerta resuelta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun reopen(id: Long): Alert? {
        val alert = alertRepository.findById(id).orElse(null)
        if (alert == null) {
            logger.warn("Alert not found for reopening: ID=$id")
            return null
        }

        if (!alert.isResolved) {
            logger.warn("Alert is not resolved, cannot reopen: ID=$id")
            return alert
        }

        val reopened = alert.copy(
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            updatedAt = Instant.now()
        )

        logger.info("Reopening alert: ID=$id")
        return alertRepository.save(reopened)
    }

    /**
     * Elimina una alerta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun delete(id: Long): Boolean {
        if (!alertRepository.existsById(id)) {
            logger.warn("Alert not found for deletion: ID=$id")
            return false
        }

        logger.info("Deleting alert: ID=$id")
        alertRepository.deleteById(id)
        return true
    }

    // ========== Métodos para TenantAlertController ==========

    /**
     * Obtiene todas las alertas de un tenant como DTOs
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun findAllByTenantId(tenantId: Long): List<AlertResponse> {
        logger.debug("Finding all alerts for tenant: $tenantId")
        return alertRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    /**
     * Busca una alerta por ID validando que pertenece al tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun findByIdAndTenantId(id: Long, tenantId: Long): AlertResponse? {
        val alert = alertRepository.findById(id).orElse(null) ?: return null
        if (alert.tenantId != tenantId) return null
        return alert.toResponse()
    }

    /**
     * Crea una nueva alerta para un tenant específico.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     * Ref: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun createForTenant(tenantId: Long, request: AlertCreateRequest): AlertResponse {
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("Invernadero no encontrado")

        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al cliente especificado")
        }

        val alert = Alert(
            code = codeGeneratorService.generateAlertCode(),
            tenantId = tenantId,
            greenhouseId = request.greenhouseId,
            alertTypeId = request.alertTypeId,
            severityId = request.severityId,
            message = request.message
        )

        logger.info("Creating alert for tenant: $tenantId, greenhouse: ${request.greenhouseId}")
        val savedAlert = alertRepository.save(alert)
        // Reload with EntityGraph to load lazy relations
        val alertId = savedAlert.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        return alertRepository.findById(alertId).orElseThrow().toResponse()
    }

    /**
     * Actualiza una alerta validando que pertenece al tenant.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun updateForTenant(id: Long, tenantId: Long, request: AlertUpdateRequest): AlertResponse? {
        val alert = alertRepository.findById(id).orElse(null) ?: return null
        if (alert.tenantId != tenantId) return null

        val updatedAlert = alert.copy(
            alertTypeId = request.alertTypeId ?: alert.alertTypeId,
            severityId = request.severityId ?: alert.severityId,
            message = request.message ?: alert.message,
            updatedAt = Instant.now()
        )

        logger.info("Updating alert: ID=$id for tenant: $tenantId")
        alertRepository.save(updatedAlert)
        // Reload with EntityGraph to load lazy relations
        return alertRepository.findById(id).orElseThrow().toResponse()
    }

    /**
     * Elimina una alerta validando que pertenece al tenant
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun deleteForTenant(id: Long, tenantId: Long): Boolean {
        val alert = alertRepository.findById(id).orElse(null) ?: return false
        if (alert.tenantId != tenantId) return false

        logger.info("Deleting alert: ID=$id for tenant: $tenantId")
        alertRepository.delete(alert)
        return true
    }

    /**
     * Resuelve una alerta validando que pertenece al tenant.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun resolveForTenant(id: Long, tenantId: Long, resolvedByUserId: Long?): AlertResponse? {
        val alert = alertRepository.findById(id).orElse(null) ?: return null
        if (alert.tenantId != tenantId) return null

        if (alert.isResolved) {
            logger.warn("Alert already resolved: ID=$id")
            return alert.toResponse()
        }

        val resolvedAlert = alert.copy(
            isResolved = true,
            resolvedAt = Instant.now(),
            resolvedByUserId = resolvedByUserId,
            updatedAt = Instant.now()
        )

        logger.info("Resolving alert: ID=$id for tenant: $tenantId, resolvedByUserId=$resolvedByUserId")
        alertRepository.save(resolvedAlert)
        // Reload with EntityGraph to load lazy relations
        return alertRepository.findById(id).orElseThrow().toResponse()
    }

    /**
     * Reabre una alerta validando que pertenece al tenant.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun reopenForTenant(id: Long, tenantId: Long): AlertResponse? {
        val alert = alertRepository.findById(id).orElse(null) ?: return null
        if (alert.tenantId != tenantId) return null

        if (!alert.isResolved) {
            logger.warn("Alert is not resolved, cannot reopen: ID=$id")
            return alert.toResponse()
        }

        val reopenedAlert = alert.copy(
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            updatedAt = Instant.now()
        )

        logger.info("Reopening alert: ID=$id for tenant: $tenantId")
        alertRepository.save(reopenedAlert)
        // Reload with EntityGraph to load lazy relations
        return alertRepository.findById(id).orElseThrow().toResponse()
    }

}
