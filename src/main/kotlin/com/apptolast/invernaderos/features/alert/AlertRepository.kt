package com.apptolast.invernaderos.features.alert

import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlertRepository : JpaRepository<Alert, UUID> {

  /** Busca alertas por tenant Ej: findByTenantId(tenantId) → List<Alert> */
  @EntityGraph(value = "Alert.context") fun findByTenantId(tenantId: UUID): List<Alert>

  /** Busca alertas por greenhouse */
  @EntityGraph(value = "Alert.context") fun findByGreenhouseId(greenhouseId: UUID): List<Alert>

  /** Busca alertas por tenant y greenhouse */
  @EntityGraph(value = "Alert.context")
  fun findByTenantIdAndGreenhouseId(tenantId: UUID, greenhouseId: UUID): List<Alert>

  /** Busca alertas no resueltas por tenant */
  @EntityGraph(value = "Alert.context")
  fun findByTenantIdAndIsResolvedFalse(tenantId: UUID): List<Alert>

  /** Busca alertas no resueltas por greenhouse */
  @EntityGraph(value = "Alert.context")
  fun findByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): List<Alert>

  /** Busca alertas por severidad */
  fun findBySeverity(severity: String): List<Alert>

  /** Busca alertas por tipo */
  fun findByAlertType(alertType: String): List<Alert>

  /** Busca alertas por device (nullable) */
  fun findByDeviceId(deviceId: UUID): List<Alert>

  /** Busca alertas creadas después de una fecha */
  fun findByCreatedAtAfter(createdAt: Instant): List<Alert>

  /** Busca alertas en rango de fechas */
  fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): List<Alert>

  /**
   * Busca alertas no resueltas por tenant ordenadas por severidad y fecha CRITICAL primero, luego
   * ERROR, WARNING, INFO
   */
  @EntityGraph(value = "Alert.context")
  @Query(
          """
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
    """
  )
  fun findUnresolvedByTenantOrderedBySeverity(@Param("tenantId") tenantId: UUID): List<Alert>

  /** Busca alertas no resueltas por greenhouse ordenadas por severidad */
  @EntityGraph(value = "Alert.context")
  @Query(
          """
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
    """
  )
  fun findUnresolvedByGreenhouseOrderedBySeverity(
          @Param("greenhouseId") greenhouseId: UUID
  ): List<Alert>

  /** Cuenta alertas no resueltas por tenant */
  fun countByTenantIdAndIsResolvedFalse(tenantId: UUID): Long

  /** Cuenta alertas no resueltas por greenhouse */
  fun countByGreenhouseIdAndIsResolvedFalse(greenhouseId: UUID): Long

  /** Cuenta alertas críticas no resueltas por tenant */
  @Query(
          """
        SELECT COUNT(a) FROM Alert a
        WHERE a.tenantId = :tenantId
          AND a.isResolved = FALSE
          AND a.severity = 'CRITICAL'
    """
  )
  fun countCriticalUnresolvedByTenant(@Param("tenantId") tenantId: UUID): Long

  /** Busca las últimas N alertas por tenant */
  @EntityGraph(value = "Alert.context")
  @Query(
          """
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
        ORDER BY a.createdAt DESC
        LIMIT :limit
    """
  )
  fun findRecentByTenant(@Param("tenantId") tenantId: UUID, @Param("limit") limit: Int): List<Alert>

  /**
   * Busca alertas por tenant, greenhouse, severidad y estado Query flexible para filtros combinados
   */
  @EntityGraph(value = "Alert.context")
  @Query(
          """
        SELECT a FROM Alert a
        WHERE a.tenantId = :tenantId
          AND (:greenhouseId IS NULL OR a.greenhouseId = :greenhouseId)
          AND (:severity IS NULL OR a.severity = :severity)
          AND (:isResolved IS NULL OR a.isResolved = :isResolved)
        ORDER BY a.createdAt DESC
    """
  )
  fun findByFilters(
          @Param("tenantId") tenantId: UUID,
          @Param("greenhouseId") greenhouseId: UUID?,
          @Param("severity") severity: String?,
          @Param("isResolved") isResolved: Boolean?
  ): List<Alert>
}
