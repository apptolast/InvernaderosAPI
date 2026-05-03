package com.apptolast.invernaderos.features.notification.domain.model

import java.time.ZoneId

/**
 * Immutable per-user notification preferences.
 *
 * [minAlertSeverity] maps to AlertSeverity.level: 1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL.
 * A notification whose severity level is strictly below [minAlertSeverity] is dropped.
 */
data class UserNotificationPreferences(
    val userId: Long,
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    val minAlertSeverity: Int,
    val quietHours: QuietHours,
    val preferredChannel: PreferredChannel
) {
    companion object {
        private val DEFAULT_TIMEZONE = ZoneId.of("Europe/Madrid")

        /** Creates a preferences object with sensible defaults for a new user (all categories enabled, no quiet hours). */
        fun default(userId: Long) = UserNotificationPreferences(
            userId = userId,
            categoryAlerts = true,
            categoryDevices = true,
            categorySubscription = true,
            minAlertSeverity = 1,
            quietHours = QuietHours(start = null, end = null, timezone = DEFAULT_TIMEZONE),
            preferredChannel = PreferredChannel.PUSH
        )
    }
}
