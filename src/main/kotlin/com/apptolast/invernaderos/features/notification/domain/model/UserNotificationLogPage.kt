package com.apptolast.invernaderos.features.notification.domain.model

/**
 * Cursor-based pagination result for [NotificationLogEntry] queries.
 *
 * [nextCursor] holds the TSID of the last entry in [entries] and should be passed
 * as the `cursor` parameter in the next page request.
 * A null [nextCursor] means there are no further pages.
 */
data class UserNotificationLogPage(
    val entries: List<NotificationLogEntry>,
    val nextCursor: Long?
)
