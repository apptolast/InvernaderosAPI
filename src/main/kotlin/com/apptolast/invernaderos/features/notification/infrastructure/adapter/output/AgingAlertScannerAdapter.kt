package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingAlertScannerPort
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingCandidate
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class AgingAlertScannerAdapter : AgingAlertScannerPort {

    @PersistenceContext(unitName = "metadataPersistenceUnit")
    private lateinit var entityManager: EntityManager

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun scan(thresholds: AgingThresholdsConfig): List<AgingCandidate> {
        val configuredLevels = thresholds.configuredLevels()
        if (configuredLevels.isEmpty()) return emptyList()

        val minLevel = configuredLevels.min()

        @Suppress("SqlNoDataSourceInspection")
        val sql = """
            SELECT
                a.id           AS alert_id,
                a.code         AS alert_code,
                a.tenant_id,
                a.sector_id,
                a.alert_type_id,
                a.severity_id,
                s.level        AS severity_level,
                s.name         AS severity_name,
                s.color        AS severity_color,
                s.notify_push  AS notify_push,
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
              AND s.level >= :minLevel
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        query.setParameter("minLevel", minLevel)

        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any?>>

        val now = Instant.now()
        val candidates = mutableListOf<AgingCandidate>()

        for (row in rows) {
            try {
                val alertId = toLong(row[0]) ?: continue
                val alertCode = row[1] as? String ?: continue
                val tenantId = toLong(row[2]) ?: continue
                val sectorId = toLong(row[3]) ?: continue
                val alertTypeId = toShort(row[4])
                val severityId = toShort(row[5]) ?: continue
                val severityLevel = toShort(row[6]) ?: continue
                val severityName = row[7] as? String ?: continue
                // color at index 8, notify_push at index 9
                val message = row[10] as? String
                val description = row[11] as? String
                val clientName = row[12] as? String
                val isResolved = row[13] as? Boolean ?: false
                val resolvedAt = (row[14] as? Timestamp)?.toInstant()
                val resolvedByUserId = toLong(row[15])
                val createdAt = (row[16] as? Timestamp)?.toInstant() ?: continue
                val updatedAt = (row[17] as? Timestamp)?.toInstant() ?: Instant.now()
                val lastActivationAt = (row[18] as? Timestamp)?.toInstant() ?: createdAt

                val threshold = thresholds.thresholdFor(severityLevel) ?: continue
                val ageMinutes = ChronoUnit.MINUTES.between(lastActivationAt, now)
                val thresholdMinutes = threshold.toMinutes()

                if (ageMinutes < thresholdMinutes) continue

                val alert = Alert(
                    id = alertId,
                    code = alertCode,
                    tenantId = TenantId(tenantId),
                    sectorId = SectorId(sectorId),
                    sectorCode = null,
                    alertTypeId = alertTypeId,
                    alertTypeName = null,
                    severityId = severityId,
                    severityName = severityName,
                    severityLevel = severityLevel,
                    message = message,
                    description = description,
                    clientName = clientName,
                    isResolved = isResolved,
                    resolvedAt = resolvedAt,
                    resolvedByUserId = resolvedByUserId,
                    resolvedByUserName = null,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )

                candidates.add(
                    AgingCandidate(
                        alert = alert,
                        lastActivationAt = lastActivationAt,
                        ageMinutes = ageMinutes,
                        thresholdMinutes = thresholdMinutes.toInt(),
                        severityName = severityName
                    )
                )
            } catch (ex: Exception) {
                logger.warn("Skipping malformed aging candidate row: {}", ex.message)
            }
        }

        logger.debug("AgingAlertScannerAdapter: scanned {} candidates from DB, {} exceeded threshold", rows.size, candidates.size)
        return candidates
    }

    private fun toLong(value: Any?): Long? = when (value) {
        null -> null
        is Long -> value
        is Int -> value.toLong()
        is BigInteger -> value.toLong()
        is Number -> value.toLong()
        else -> null
    }

    private fun toShort(value: Any?): Short? = when (value) {
        null -> null
        is Short -> value
        is Int -> value.toShort()
        is Long -> value.toShort()
        is Number -> value.toShort()
        else -> null
    }
}
