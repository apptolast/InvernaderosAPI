package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage
import java.time.Instant

/**
 * Driven port for appending and reading the notification audit log.
 *
 * The log is append-only: [save] always inserts a new row, never updates.
 */
interface NotificationLogRepositoryPort {
    fun save(entry: NotificationLogEntry): NotificationLogEntry

    fun listForUser(userId: Long, cursor: Long?, limit: Int): UserNotificationLogPage

    /**
     * Returns true if there is already a SENT log entry for the given [notificationType]
     * and [alertId] created after [sinceInstant].
     *
     * Used by [com.apptolast.invernaderos.features.notification.application.usecase.DetectAgingAlertsUseCaseImpl]
     * to avoid re-emitting aging events for the same activation window.
     */
    fun hasRecentSent(notificationType: NotificationType, alertId: Long, sinceInstant: Instant): Boolean
}
