package com.apptolast.invernaderos.features.notification.domain.port.output

/**
 * Driven port for resolving basic user information needed during notification dispatch.
 *
 * Returns null when the user does not exist; callers should treat missing users as a
 * signal to skip dispatching to the associated token and log the skip reason.
 */
interface UserLookupPort {
    fun findById(userId: Long): NotificationUserSnapshot?
}

/**
 * Read-only projection of a user, used exclusively within the notification domain.
 *
 * [locale] is a BCP-47 language tag (e.g. "es-ES", "en-US") used to select the
 * i18n bundle when rendering notification content.
 */
data class NotificationUserSnapshot(
    val id: Long,
    val username: String?,
    val displayName: String?,
    val locale: String,
    val tenantId: Long
)
