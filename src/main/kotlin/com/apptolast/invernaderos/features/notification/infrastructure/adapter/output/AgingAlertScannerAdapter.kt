package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingAlertScannerPort
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingCandidate
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class AgingAlertScannerAdapter(
    @Qualifier("metadataJdbcTemplate")
    private val jdbcTemplate: JdbcTemplate
) : AgingAlertScannerPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun scan(thresholds: AgingThresholdsConfig): List<AgingCandidate> {
        val configuredLevels = thresholds.configuredLevels()
        if (configuredLevels.isEmpty()) return emptyList()

        val minLevel = configuredLevels.min()

        val sql = """
            SELECT
                a.id               AS alert_id,
                a.code             AS alert_code,
                a.tenant_id,
                a.sector_id,
                a.alert_type_id,
                a.severity_id,
                s.level            AS severity_level,
                s.name             AS severity_name,
                a.message,
                a.description,
                a.client_name,
                a.is_resolved,
                a.resolved_at,
                a.resolved_by_user_id,
                a.created_at,
                a.updated_at,
                (
                    SELECT MAX(asc1.at)
                    FROM metadata.alert_state_changes asc1
                    WHERE asc1.alert_id = a.id
                      AND asc1.to_resolved = false
                ) AS last_activation_at
            FROM metadata.alerts a
            JOIN metadata.alert_severities s ON s.id = a.severity_id
            WHERE a.is_resolved = false
              AND s.level >= ?
        """.trimIndent()

        val now = Instant.now()
        val candidates = mutableListOf<AgingCandidate>()

        try {
            val rows = jdbcTemplate.query(sql, { rs, _ -> mapRow(rs) }, minLevel.toInt())

            for (row in rows) {
                val lastActivationAt = row.lastActivationAt ?: row.createdAt
                val severityLevel = row.severityLevel
                val threshold = thresholds.thresholdFor(severityLevel) ?: continue
                val ageMinutes = ChronoUnit.MINUTES.between(lastActivationAt, now)
                val thresholdMinutes = threshold.toMinutes()

                if (ageMinutes < thresholdMinutes) continue

                val alert = Alert(
                    id = row.alertId,
                    code = row.alertCode,
                    tenantId = TenantId(row.tenantId),
                    sectorId = SectorId(row.sectorId),
                    sectorCode = null,
                    alertTypeId = row.alertTypeId,
                    alertTypeName = null,
                    severityId = row.severityId,
                    severityName = row.severityName,
                    severityLevel = row.severityLevel,
                    message = row.message,
                    description = row.description,
                    clientName = row.clientName,
                    isResolved = row.isResolved,
                    resolvedAt = row.resolvedAt,
                    resolvedByUserId = row.resolvedByUserId,
                    resolvedByUserName = null,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt
                )

                candidates.add(
                    AgingCandidate(
                        alert = alert,
                        lastActivationAt = lastActivationAt,
                        ageMinutes = ageMinutes,
                        thresholdMinutes = thresholdMinutes.toInt(),
                        severityName = row.severityName
                    )
                )
            }
        } catch (ex: Exception) {
            logger.error("AgingAlertScannerAdapter: error scanning aging alerts", ex)
        }

        logger.debug(
            "AgingAlertScannerAdapter: {} candidates exceed threshold out of all unresolved alerts at minLevel={}",
            candidates.size, minLevel
        )
        return candidates
    }

    private data class AlertRow(
        val alertId: Long,
        val alertCode: String,
        val tenantId: Long,
        val sectorId: Long,
        val alertTypeId: Short?,
        val severityId: Short,
        val severityLevel: Short,
        val severityName: String,
        val message: String?,
        val description: String?,
        val clientName: String?,
        val isResolved: Boolean,
        val resolvedAt: Instant?,
        val resolvedByUserId: Long?,
        val createdAt: Instant,
        val updatedAt: Instant,
        val lastActivationAt: Instant?
    )

    private fun mapRow(rs: ResultSet): AlertRow = AlertRow(
        alertId = rs.getLong("alert_id"),
        alertCode = rs.getString("alert_code"),
        tenantId = rs.getLong("tenant_id"),
        sectorId = rs.getLong("sector_id"),
        alertTypeId = rs.getShort("alert_type_id").takeUnless { rs.wasNull() },
        severityId = rs.getShort("severity_id"),
        severityLevel = rs.getShort("severity_level"),
        severityName = rs.getString("severity_name"),
        message = rs.getString("message"),
        description = rs.getString("description"),
        clientName = rs.getString("client_name"),
        isResolved = rs.getBoolean("is_resolved"),
        resolvedAt = rs.getTimestamp("resolved_at")?.toInstant(),
        resolvedByUserId = rs.getLong("resolved_by_user_id").takeUnless { rs.wasNull() },
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        lastActivationAt = rs.getTimestamp("last_activation_at")?.toInstant()
    )
}
