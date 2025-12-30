package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.alert.Alert
import com.apptolast.invernaderos.features.alert.AlertRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

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
    private val alertRepository: AlertRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todas las alertas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenant(tenantId: UUID): List<Alert> {
        logger.debug("Getting all alerts for tenant: $tenantId")
        return alertRepository.findByTenantId(tenantId)
    }

    /**
     * Obtiene alertas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByGreenhouse(greenhouseId: UUID): List<Alert> {
        logger.debug("Getting all alerts for greenhouse: $greenhouseId")
        return alertRepository.findByGreenhouseId(greenhouseId)
    }

    /**
     * Obtiene alertas por tenant y greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenantAndGreenhouse(tenantId: UUID, greenhouseId: UUID): List<Alert> {
        logger.debug("Getting alerts for tenant: $tenantId, greenhouse: $greenhouseId")
        return alertRepository.findByTenantIdAndGreenhouseId(tenantId, greenhouseId)
    }

    /**
     * Obtiene alertas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByTenant(tenantId: UUID): List<Alert> {
        logger.debug("Getting unresolved alerts for tenant: $tenantId")
        return alertRepository.findByTenantIdAndIsResolvedFalse(tenantId)
    }

    /**
     * Obtiene alertas no resueltas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByGreenhouse(greenhouseId: UUID): List<Alert> {
        logger.debug("Getting unresolved alerts for greenhouse: $greenhouseId")
        return alertRepository.findByGreenhouseIdAndIsResolvedFalse(greenhouseId)
    }

    /**
     * Obtiene alertas no resueltas ordenadas por severidad
     * CRITICAL primero, luego ERROR, WARNING, INFO
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByTenantOrderedBySeverity(tenantId: UUID): List<Alert> {
        logger.debug("Getting unresolved alerts ordered by severity for tenant: $tenantId")
        return alertRepository.findUnresolvedByTenantOrderedBySeverity(tenantId)
    }

    /**
     * Obtiene alertas no resueltas por greenhouse ordenadas por severidad
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedByGreenhouseOrderedBySeverity(greenhouseId: UUID): List<Alert> {
        logger.debug("Getting unresolved alerts ordered by severity for greenhouse: $greenhouseId")
        return alertRepository.findUnresolvedByGreenhouseOrderedBySeverity(greenhouseId)
    }

    /**
     * Busca alertas por device
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getByDevice(deviceId: UUID): List<Alert> {
        logger.debug("Getting alerts for device: $deviceId")
        return alertRepository.findByDeviceId(deviceId)
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
        tenantId: UUID,
        greenhouseId: UUID?,
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
    fun countUnresolvedByTenant(tenantId: UUID): Long {
        return alertRepository.countByTenantIdAndIsResolvedFalse(tenantId)
    }

    /**
     * Cuenta alertas no resueltas por greenhouse
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countUnresolvedByGreenhouse(greenhouseId: UUID): Long {
        return alertRepository.countByGreenhouseIdAndIsResolvedFalse(greenhouseId)
    }

    /**
     * Cuenta alertas críticas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countCriticalUnresolvedByTenant(tenantId: UUID): Long {
        return alertRepository.countCriticalUnresolvedByTenant(tenantId)
    }

    /**
     * Obtiene las últimas N alertas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getRecentByTenant(tenantId: UUID, limit: Int = 50): List<Alert> {
        logger.debug("Getting recent $limit alerts for tenant: $tenantId")
        return alertRepository.findRecentByTenant(tenantId, limit)
    }

    /**
     * Busca una alerta por ID
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getById(id: UUID): Alert? {
        logger.debug("Getting alert by ID: $id")
        return alertRepository.findById(id).orElse(null)
    }

    /**
     * Crea una nueva alerta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun create(alert: Alert): Alert {
        logger.info("Creating new alert: type=${alert.alertType}, severity=${alert.severity}, greenhouse=${alert.greenhouseId}")
        return alertRepository.save(alert)
    }

    /**
     * Actualiza una alerta existente
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun update(id: UUID, alert: Alert): Alert? {
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
    fun resolve(id: UUID, resolvedByUserId: UUID?, resolvedBy: String? = null): Alert? {
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
            resolvedBy = resolvedBy,
            updatedAt = Instant.now()
        )

        logger.info("Resolving alert: ID=$id, resolvedBy=$resolvedBy")
        return alertRepository.save(resolved)
    }

    /**
     * Reabre una alerta resuelta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun reopen(id: UUID): Alert? {
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
            resolvedBy = null,
            updatedAt = Instant.now()
        )

        logger.info("Reopening alert: ID=$id")
        return alertRepository.save(reopened)
    }

    /**
     * Elimina una alerta
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun delete(id: UUID): Boolean {
        if (!alertRepository.existsById(id)) {
            logger.warn("Alert not found for deletion: ID=$id")
            return false
        }

        logger.info("Deleting alert: ID=$id")
        alertRepository.deleteById(id)
        return true
    }

}
