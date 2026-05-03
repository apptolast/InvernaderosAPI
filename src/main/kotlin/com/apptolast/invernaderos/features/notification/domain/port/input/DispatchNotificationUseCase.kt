package com.apptolast.invernaderos.features.notification.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.shared.domain.Either

/**
 * Driving port that orchestrates the end-to-end dispatch of a push notification.
 *
 * Resolves severity, fetches all active tenant tokens, applies per-user preference
 * filtering (category, min-severity, quiet hours, dedup), renders i18n content, and
 * sends via FCM. Every dispatch attempt — sent or dropped — is appended to the
 * notification log.
 *
 * [change] is null only for [NotificationType.ALERT_AGING] events; it is always
 * present for [NotificationType.ALERT_ACTIVATED] and [NotificationType.ALERT_RESOLVED].
 *
 * [agingContext] is non-null only for [NotificationType.ALERT_AGING] dispatches; it
 * carries the data the renderer needs to format the aging-specific title/body.
 */
interface DispatchNotificationUseCase {
    fun dispatch(
        type: NotificationType,
        alert: Alert,
        change: AlertStateChange?,
        agingContext: AlertAgingDetectedEvent? = null
    ): Either<NotificationError, DispatchSummary>
}

/**
 * High-level summary of one [DispatchNotificationUseCase.dispatch] call.
 *
 * [sent] is the number of FCM messages successfully accepted.
 * [dropped] is the number of tokens skipped due to preference/quiet-hours/dedup filters.
 * [failed] is the number of tokens for which FCM returned a non-retryable error.
 */
data class DispatchSummary(
    val sent: Int,
    val dropped: Int,
    val failed: Int
)
