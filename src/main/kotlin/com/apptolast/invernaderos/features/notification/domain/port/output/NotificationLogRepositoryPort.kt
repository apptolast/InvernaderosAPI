package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage

/**
 * Driven port for appending and reading the notification audit log.
 *
 * The log is append-only: [save] always inserts a new row, never updates.
 */
interface NotificationLogRepositoryPort {
    fun save(entry: NotificationLogEntry): NotificationLogEntry

    fun listForUser(userId: Long, cursor: Long?, limit: Int): UserNotificationLogPage
}
