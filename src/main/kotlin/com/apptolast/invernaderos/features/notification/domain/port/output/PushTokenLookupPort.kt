package com.apptolast.invernaderos.features.notification.domain.port.output

/**
 * Driven port for retrieving the active FCM tokens registered for a tenant.
 *
 * Returns lightweight [NotificationTokenSnapshot] objects to avoid coupling to the
 * push-token aggregate from the legacy [com.apptolast.invernaderos.features.push] module.
 */
interface PushTokenLookupPort {
    fun findActiveTokensForTenant(tenantId: Long): List<NotificationTokenSnapshot>
}

/**
 * Read-only projection of a push token, used exclusively within the notification domain.
 *
 * [platform] mirrors the platform stored in the push_tokens table (ANDROID, IOS, WEB).
 */
data class NotificationTokenSnapshot(
    val id: Long,
    val userId: Long,
    val token: String,
    val platform: String
)
