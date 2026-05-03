package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.input.DetectAgingAlertsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingAlertScannerPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort

/**
 * Scans for active alerts whose age exceeds the configured threshold and have not yet
 * triggered an aging notification within the current activation window.
 *
 * Idempotency: for each [AgingCandidate] returned by the scanner, this use case checks
 * whether a SENT log entry already exists for the same alert since its [lastActivationAt].
 * If one is found, the candidate is silently skipped. This ensures the scheduler can run
 * frequently without generating duplicate aging notifications.
 *
 * The returned [AlertAgingDetectedEvent] list is handed back to the scheduler adapter,
 * which is responsible for publishing each event to the Spring application event bus.
 */
class DetectAgingAlertsUseCaseImpl(
    private val agingAlertScanner: AgingAlertScannerPort,
    private val notificationLogRepository: NotificationLogRepositoryPort,
    private val thresholdsConfig: AgingThresholdsConfig
) : DetectAgingAlertsUseCase {

    override fun detect(): List<AlertAgingDetectedEvent> {
        val candidates = agingAlertScanner.scan(thresholdsConfig)

        return candidates.mapNotNull { candidate ->
            val alertId = candidate.alert.id ?: return@mapNotNull null

            val alreadyNotified = notificationLogRepository.hasRecentSent(
                notificationType = NotificationType.ALERT_AGING,
                alertId = alertId,
                sinceInstant = candidate.lastActivationAt
            )

            if (alreadyNotified) return@mapNotNull null

            AlertAgingDetectedEvent(
                alertId = alertId,
                alertCode = candidate.alert.code,
                tenantId = candidate.alert.tenantId.value,
                severityId = candidate.alert.severityId ?: return@mapNotNull null,
                severityLevel = candidate.alert.severityLevel ?: return@mapNotNull null,
                severityName = candidate.severityName,
                activatedAt = candidate.lastActivationAt,
                ageMinutes = candidate.ageMinutes,
                thresholdMinutes = candidate.thresholdMinutes
            )
        }
    }
}
