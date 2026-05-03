package com.apptolast.invernaderos.features.notification.domain.port.input

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences

/**
 * Driving port for retrieving the notification preferences of a specific user.
 *
 * If no preferences row exists yet, implementations must create and persist defaults
 * (via [com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort.save])
 * and return them. This is an upsert-on-read pattern: the caller always receives a valid object.
 */
interface GetUserPreferencesUseCase {
    fun get(userId: Long): UserNotificationPreferences
}
