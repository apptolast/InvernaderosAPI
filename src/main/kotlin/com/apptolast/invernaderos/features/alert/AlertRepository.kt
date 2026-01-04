package com.apptolast.invernaderos.features.alert

import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlertRepository : JpaRepository<Alert, UUID> {

    /**
     * Override findById para cargar relaciones con EntityGraph.
     * Según documentación oficial Spring Data JPA:
     * https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html
     */
    @EntityGraph(value = "Alert.context")
    override fun findById(id: UUID): Optional<Alert>

    /**
     * Busca alertas por tenant.
     */
    @EntityGraph(value = "Alert.context")
    fun findByTenantId(tenantId: UUID): List<Alert>

    /**
     * Busca alertas por greenhouse.
     */
    @EntityGraph(value = "Alert.context")
    fun findByGreenhouseId(greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas por tenant y greenhouse.
     */
    @EntityGraph(value = "Alert.context")
    fun findByTenantIdAndGreenhouseId(tenantId: UUID, greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por tenant.
     */
    @EntityGraph(value = "Alert.context")
    fun findByTenantIdAndIsResolvedFalse(tenantId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por greenhouse.
     */
    @EntityGraph(value = "Alert.context")
    fun findByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): List<Alert>

    /**
     * Busca alertas por severidad ID.
     */
    fun findBySeverityId(severityId: Short): List<Alert>

    /**
     * Busca alertas por tipo de alerta ID.
     */
    fun findByAlertTypeId(alertTypeId: Short): List<Alert>

    /**
     * Busca alertas creadas despues de una fecha.
     */
    fun findByCreatedAtAfter(createdAt: Instant): List<Alert>

    /**
     * Busca alertas en rango de fechas.
     */
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): List<Alert>

    /**
     * Busca alertas no resueltas por tenant ordenadas por severidad.
     * Usa JOIN con alert_severities para ordenar por level.
     */
    @EntityGraph(value = "Alert.context")
    @Query("""
        SELECT a FROM Alert a
        LEFT JOIN a.severity s
        WHERE a.tenantId = :tenantId
          AND a.isResolved = FALSE
        ORDER BY s.level DESC NULLS LAST, a.createdAt DESC
    """)
    fun findUnresolvedByTenantOrderedBySeverity(@Param("tenantId") tenantId: UUID): List<Alert>

    /**
     * Busca alertas no resueltas por greenhouse ordenadas por severidad.
     */
    @EntityGraph(value = "Alert.context")
    @Query("""
        SELECT a FROM Alert a
        LEFT JOIN a.severity s
        WHERE a.greenhouseId = :greenhouseId
          AND a.isResolved = FALSE
        ORDER BY s.level DESC NULLS LAST, a.createdAt DESC
    """)
    fun findUnresolvedByGreenhouseOrderedBySeverity(@Param("greenhouseId") greenhouseId: UUID): List<Alert>

    /**
     * Cuenta alertas no resueltas por tenant.
     */
    fun countByTenantIdAndIsResolvedFalse(tenantId: UUID): Long

    /**
     * Cuenta alertas no resueltas por greenhouse.
     */
    fun countByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): Long

    /**
     * Cuenta alertas criticas no resueltas por tenant.
     * Requiere JOIN con alert_severities para filtrar por nombre.
     */
    @Query("""
        SELECT COUNT(a) FROM Alert a
        JOIN a.severity s
        WHERE a.tenantId = :tenantId
          AND a.isResolved = FALSE
          AND s.name = 'CRITICAL'
    """)
    fun countCriticalUnresolvedByTenant(@Param("tenantId") tenantId: UUID): Long

    /**
     * Busca las ultimas N alertas por tenant.
     */
    @EntityGraph(value = "Alert.context")
    @Query("""
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
        ORDER BY a.createdAt DESC
        LIMIT :limit
    """)
    fun findRecentByTenant(@Param("tenantId") tenantId: UUID, @Param("limit") limit: Int): List<Alert>

    /**
     * Busca alertas por tenant, greenhouse, severidad y estado.
     * Query flexible para filtros combinados.
     */
    @EntityGraph(value = "Alert.context")
    @Query("""
        SELECT a FROM Alert a
        JOIN a.severity s
        WHERE a.tenantId = :tenantId
          AND (:greenhouseId IS NULL OR a.greenhouseId = :greenhouseId)
          AND (:severityName IS NULL OR s.name = :severityName)
          AND (:isResolved IS NULL OR a.isResolved = :isResolved)
        ORDER BY a.createdAt DESC
    """)
    fun findByFilters(
        @Param("tenantId") tenantId: UUID,
        @Param("greenhouseId") greenhouseId: UUID?,
        @Param("severityName") severityName: String?,
        @Param("isResolved") isResolved: Boolean?
    ): List<Alert>

}
