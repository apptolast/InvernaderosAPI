package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.alert.Alert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface AlertRepository : JpaRepository<Alert, UUID> {

    /**
     * Busca alertas por tenant
     * Ej: findByTenantId(tenantId) → List<Alert>
     */
    fun findByTenantId(tenantId: UUID): List<Alert>

    /**
     * Busca alertas por greenhouse
     */
    fun findByGreenhouseId(greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas por tenant y greenhouse
     */
    fun findByTenantIdAndGreenhouseId(tenantId: UUID, greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por tenant
     */
    fun findByTenantIdAndIsResolvedFalse(tenantId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por greenhouse
     */
    fun findByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas por severidad
     */
    fun findBySeverity(severity: String): List<Alert>

    /**
     * Busca alertas por tipo
     */
    fun findByAlertType(alertType: String): List<Alert>

    /**
     * Busca alertas por sensor
     */
    fun findBySensorId(sensorId: UUID): List<Alert>

    /**
     * Busca alertas creadas después de una fecha
     */
    fun findByCreatedAtAfter(createdAt: Instant): List<Alert>

    /**
     * Busca alertas en rango de fechas
     */
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): List<Alert>

    /**
     * Busca alertas no resueltas por tenant ordenadas por severidad y fecha
     * CRITICAL primero, luego ERROR, WARNING, INFO
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
          AND a.isResolved = FALSE
        ORDER BY
          CASE a.severity
            WHEN 'CRITICAL' THEN 1
            WHEN 'ERROR' THEN 2
            WHEN 'WARNING' THEN 3
            WHEN 'INFO' THEN 4
            ELSE 5
          END ASC,
          a.createdAt DESC
    """)
    fun findUnresolvedByTenantOrderedBySeverity(@Param("tenantId") tenantId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por greenhouse ordenadas por severidad
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.greenhouseId = :greenhouseId
          AND a.isResolved = FALSE
        ORDER BY
          CASE a.severity
            WHEN 'CRITICAL' THEN 1
            WHEN 'ERROR' THEN 2
            WHEN 'WARNING' THEN 3
            WHEN 'INFO' THEN 4
            ELSE 5
          END ASC,
          a.createdAt DESC
    """)
    fun findUnresolvedByGreenhouseOrderedBySeverity(@Param("greenhouseId") greenhouseId: UUID): List<Alert>

    /**
     * Cuenta alertas no resueltas por tenant
     */
    fun countByTenantIdAndIsResolvedFalse(tenantId: UUID): Long

    /**
     * Cuenta alertas no resueltas por greenhouse
     */
    fun countByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): Long

    /**
     * Cuenta alertas críticas no resueltas por tenant
     */
    @Query("""
        SELECT COUNT(a) FROM Alert a
        WHERE a.tenantId = :tenantId
          AND a.isResolved = FALSE
          AND a.severity = 'CRITICAL'
    """)
    fun countCriticalUnresolvedByTenant(@Param("tenantId") tenantId: UUID): Long

    /**
     * Busca las últimas N alertas por tenant
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
        ORDER BY a.createdAt DESC
        LIMIT :limit
    """)
    fun findRecentByTenant(@Param("tenantId") tenantId: UUID, @Param("limit") limit: Int): List<Alert>

    /**
     * Busca alertas por tenant, greenhouse, severidad y estado
     * Query flexible para filtros combinados
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
          AND (:greenhouseId IS NULL OR a.greenhouseId = :greenhouseId)
          AND (:severity IS NULL OR a.severity = :severity)
          AND (:isResolved IS NULL OR a.isResolved = :isResolved)
        ORDER BY a.createdAt DESC
    """)
    fun findByFilters(
        @Param("tenantId") tenantId: UUID,
        @Param("greenhouseId") greenhouseId: UUID?,
        @Param("severity") severity: String?,
        @Param("isResolved") isResolved: Boolean?
    ): List<Alert>

    /**
     * Busca alertas usando IDs normalizados (severityId, alertTypeId)
     */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
          AND (:severityId IS NULL OR a.severityId = :severityId)
          AND (:alertTypeId IS NULL OR a.alertTypeId = :alertTypeId)
          AND a.isResolved = FALSE
        ORDER BY a.severityId ASC, a.createdAt DESC
    """)
    fun findUnresolvedByNormalizedFields(
        @Param("tenantId") tenantId: UUID,
        @Param("severityId") severityId: Short?,
        @Param("alertTypeId") alertTypeId: Short?
    ): List<Alert>
}
