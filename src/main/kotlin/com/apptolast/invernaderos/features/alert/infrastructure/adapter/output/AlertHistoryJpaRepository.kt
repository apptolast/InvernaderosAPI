package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertStateChange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for alert_state_changes read-side queries.
 *
 * This repository is limited to ownership checks. All complex queries with window
 * functions or dynamic WHERE clauses are handled in [AlertHistoryQueryAdapter]
 * via the injected JdbcTemplate, which gives explicit control over the SQL and avoids
 * the friction of mapping native query projections through Spring Data.
 */
@Repository
interface AlertHistoryJpaRepository : JpaRepository<AlertStateChange, Long> {

    /**
     * Verify that an alert belongs to the given tenant before running the full
     * per-alert timeline query. Cheap: uses the PK on alert_state_changes + PK on alerts.
     */
    @Query(
        value = """
            SELECT COUNT(*)
              FROM metadata.alert_state_changes asc
              JOIN metadata.alerts a ON a.id = asc.alert_id
             WHERE asc.alert_id = :alertId
               AND a.tenant_id = :tenantId
        """,
        nativeQuery = true
    )
    fun countByAlertIdAndTenantId(
        @Param("alertId") alertId: Long,
        @Param("tenantId") tenantId: Long,
    ): Long
}
