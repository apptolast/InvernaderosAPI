package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.alert.dto.AlertCreateRequest
import com.apptolast.invernaderos.features.alert.dto.AlertResponse
import com.apptolast.invernaderos.features.alert.dto.AlertUpdateRequest
import com.apptolast.invernaderos.features.alert.dto.toResponse
import com.apptolast.invernaderos.features.sector.SectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para gestión de Alertas.
 *
 * Maneja la lógica de negocio para:
 * - Crear, actualizar, resolver alertas
 * - Buscar alertas por tenant, sector, sensor, actuador
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
    private val sectorRepository: SectorRepository,
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
     * Obtiene alertas por sector
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllBySector(sectorId: Long): List<Alert> {
        logger.debug("Getting all alerts for sector: $sectorId")
        return alertRepository.findBySectorId(sectorId)
    }

    /**
     * Obtiene alertas por tenant y sector
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenantAndSector(tenantId: Long, sectorId: Long): List<Alert> {
        logger.debug("Getting alerts for tenant: $tenantId, sector: $sectorId")
        return alertRepository.findByTenantIdAndSectorId(tenantId, sectorId)
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
     * Obtiene alertas no resueltas por sector
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedBySector(sectorId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts for sector: $sectorId")
        return alertRepository.findBySectorIdAndIsResolvedFalse(sectorId)
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
     * Obtiene alertas no resueltas por sector ordenadas por severidad
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getUnresolvedBySectorOrderedBySeverity(sectorId: Long): List<Alert> {
        logger.debug("Getting unresolved alerts ordered by severity for sector: $sectorId")
        return alertRepository.findUnresolvedBySectorOrderedBySeverity(sectorId)
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
        sectorId: Long?,
        severity: String?,
        isResolved: Boolean?
    ): List<Alert> {
        logger.debug("Getting alerts with filters - tenant: $tenantId, sector: $sectorId, severity: $severity, isResolved: $isResolved")
        return alertRepository.findByFilters(tenantId, sectorId, severity, isResolved)
    }

    /**
     * Cuenta alertas no resueltas por tenant
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countUnresolvedByTenant(tenantId: Long): Long {
        return alertRepository.countByTenantIdAndIsResolvedFalse(tenantId)
    }

    /**
     * Cuenta alertas no resueltas por sector
     */
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun countUnresolvedBySector(sectorId: Long): Long {
        return alertRepository.countBySectorIdAndIsResolvedFalse(sectorId)
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
        logger.info("Creating new alert: type=${alert.alertTypeId}, severity=${alert.severityId}, sector=${alert.sectorId}")
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
        val sector = sectorRepository.findById(request.sectorId).orElse(null)
            ?: throw IllegalArgumentException("Sector no encontrado")

        if (sector.tenantId != tenantId) {
            throw IllegalArgumentException("El sector no pertenece al cliente especificado")
        }

        val alert = Alert(
            code = codeGeneratorService.generateAlertCode(),
            tenantId = tenantId,
            sectorId = request.sectorId,
            alertTypeId = request.alertTypeId,
            severityId = request.severityId,
            message = request.message,
            description = request.description
        )

        logger.info("Creating alert for tenant: $tenantId, sector: ${request.sectorId}")
        val savedAlert = alertRepository.save(alert)
        // Reload with EntityGraph to load lazy relations
        val alertId = savedAlert.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        return alertRepository.findById(alertId).orElseThrow().toResponse()
    }

    /**
     * Actualiza una alerta validando que pertenece al tenant.
     * Si se proporciona un nuevo sectorId, valida que el sector pertenezca al mismo tenant.
     * Después de save(), usamos findById() para cargar las relaciones con EntityGraph.
     */
    @Transactional("postgreSQLTransactionManager", rollbackFor = [Exception::class])
    fun updateForTenant(id: Long, tenantId: Long, request: AlertUpdateRequest): AlertResponse? {
        val alert = alertRepository.findById(id).orElse(null) ?: return null
        if (alert.tenantId != tenantId) return null

        // Validar y obtener el nuevo sectorId si se proporciona
        val newSectorId = if (request.sectorId != null && request.sectorId != alert.sectorId) {
            val newSector = sectorRepository.findById(request.sectorId).orElse(null)
                ?: throw IllegalArgumentException("Sector no encontrado con ID: ${request.sectorId}")

            if (newSector.tenantId != tenantId) {
                throw IllegalArgumentException("El sector con ID ${request.sectorId} no pertenece al cliente especificado")
            }
            request.sectorId
        } else {
            alert.sectorId
        }

        val updatedAlert = alert.copy(
            sectorId = newSectorId,
            alertTypeId = request.alertTypeId ?: alert.alertTypeId,
            severityId = request.severityId ?: alert.severityId,
            message = request.message ?: alert.message,
            description = request.description ?: alert.description,
            updatedAt = Instant.now()
        )

        logger.info("Updating alert: ID=$id for tenant: $tenantId, sectorId: $newSectorId")
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
