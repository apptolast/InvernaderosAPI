package com.apptolast.invernaderos.features.notification.domain.port.input

import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.shared.domain.Either

/**
 * Driving port for updating the notification preferences of a specific user.
 *
 * Validates the incoming [preferences] object before persisting:
 * - [UserNotificationPreferences.minAlertSeverity] must be in 1..4.
 * - Quiet hours: if only one of start/end is set the preferences are considered invalid.
 * - [UserNotificationPreferences.preferredChannel] must be a known enum value (enforced by type).
 *
 * Returns [NotificationError.InvalidPreferences] on validation failure.
 */
interface UpdateUserPreferencesUseCase {
    fun update(userId: Long, preferences: UserNotificationPreferences): Either<NotificationError, UserNotificationPreferences>
}
