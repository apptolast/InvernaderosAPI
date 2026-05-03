package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences

/**
 * Driven port for persisting and retrieving per-user notification preferences.
 *
 * [findByUserId] returns null when no row exists yet for the given user.
 * Callers must create defaults via [save] when null is returned.
 */
interface UserPreferencesRepositoryPort {
    fun findByUserId(userId: Long): UserNotificationPreferences?
    fun save(preferences: UserNotificationPreferences): UserNotificationPreferences
}
