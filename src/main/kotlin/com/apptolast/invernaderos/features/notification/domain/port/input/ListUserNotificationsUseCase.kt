package com.apptolast.invernaderos.features.notification.domain.port.input

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage

/**
 * Driving port for retrieving a paginated view of the notification log for a specific user.
 *
 * Pagination uses a cursor-based strategy where [cursor] is the TSID of the last entry
 * seen by the client (descending order). Pass null to retrieve the most recent entries.
 * [limit] is capped at 50 by the adapter layer.
 */
interface ListUserNotificationsUseCase {
    fun list(userId: Long, cursor: Long?, limit: Int): UserNotificationLogPage
}
