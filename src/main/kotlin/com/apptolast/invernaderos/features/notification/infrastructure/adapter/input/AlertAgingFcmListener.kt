package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchNotificationUseCase
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Listens for [AlertAgingDetectedEvent] and dispatches an ALERT_AGING push notification.
 *
 * Uses @EventListener (not @TransactionalEventListener) because the aging event is produced
 * by the scheduler outside an active transaction context.
 *
 * The Alert domain object is reconstructed from the event's alertId and tenantId via
 * [AlertRepositoryPort] to provide the full context needed by [DispatchNotificationUseCase].
 *
 * Exceptions are caught globally to ensure the scheduler thread is never interrupted.
 */
@Component
class AlertAgingFcmListener(
    private val dispatchNotificationUseCase: DispatchNotificationUseCase,
    private val alertRepositoryPort: AlertRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async("fcmSendExecutor")
    fun onAgingDetected(event: AlertAgingDetectedEvent) {
        try {
            val alert = alertRepositoryPort.findByIdAndTenantId(
                id = event.alertId,
                tenantId = TenantId(event.tenantId)
            )

            if (alert == null) {
                logger.warn(
                    "AlertAgingFcmListener: alert not found alertId={} tenantId={} — notification skipped",
                    event.alertId, event.tenantId
                )
                return
            }

            val result = dispatchNotificationUseCase.dispatch(
                type = NotificationType.ALERT_AGING,
                alert = alert,
                change = null,
                agingContext = event
            )

            result.fold(
                onLeft = { error ->
                    logger.error(
                        "ALERT_AGING dispatch failed for alertId={} alertCode={}: {}",
                        event.alertId, event.alertCode, error.message
                    )
                },
                onRight = { summary ->
                    logger.info(
                        "ALERT_AGING dispatched for alertId={} alertCode={} ageMinutes={} sent={} dropped={} failed={}",
                        event.alertId, event.alertCode, event.ageMinutes,
                        summary.sent, summary.dropped, summary.failed
                    )
                }
            )
        } catch (ex: Exception) {
            logger.error(
                "Unexpected error in AlertAgingFcmListener for alertId={} alertCode={} — notification lost",
                event.alertId, event.alertCode, ex
            )
        }
    }
}
