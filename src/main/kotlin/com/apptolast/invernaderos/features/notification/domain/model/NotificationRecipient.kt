package com.apptolast.invernaderos.features.notification.domain.model

import java.util.Locale

/**
 * Represents a single push notification recipient: one user + one device token.
 * A user with multiple registered devices will appear as multiple [NotificationRecipient] instances.
 *
 * [locale] drives i18n rendering of the notification content for this recipient.
 */
data class NotificationRecipient(
    val userId: Long,
    val tokenId: Long,
    val tokenValue: String,
    val locale: Locale
)
