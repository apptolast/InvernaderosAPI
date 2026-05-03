package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchNotificationUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listens for [AlertStateChangedEvent] and dispatches an ALERT_ACTIVATED push notification
 * when an alert transitions to activated state (toResolved == false).
 *
 * Phase = AFTER_COMMIT: the push is only sent after the originating transaction commits.
 * If the transaction rolls back, no notification is sent.
 *
 * Exceptions are caught globally to ensure the event bus is never interrupted.
 */
@Component
class AlertActivatedFcmListener(
    private val dispatchNotificationUseCase: DispatchNotificationUseCase
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("fcmSendExecutor")
    fun onAlertStateChanged(event: AlertStateChangedEvent) {
        if (event.change.toResolved) {
            logger.debug(
                "AlertActivatedFcmListener: skipping RESOLUTION event for alert={}",
                event.alert.code
            )
            return
        }

        try {
            val result = dispatchNotificationUseCase.dispatch(
                type = NotificationType.ALERT_ACTIVATED,
                alert = event.alert,
                change = event.change,
                agingContext = null
            )
            result.fold(
                onLeft = { error ->
                    logger.error(
                        "ALERT_ACTIVATED dispatch failed for alert={}: {}",
                        event.alert.code, error.message
                    )
                },
                onRight = { summary ->
                    logger.info(
                        "ALERT_ACTIVATED dispatched for alert={} sent={} dropped={} failed={}",
                        event.alert.code, summary.sent, summary.dropped, summary.failed
                    )
                }
            )
        } catch (ex: Exception) {
            logger.error(
                "Unexpected error in AlertActivatedFcmListener for alert={} — notification lost, DB state already committed",
                event.alert.code, ex
            )
        }
    }
}
