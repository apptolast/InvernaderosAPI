package com.apptolast.invernaderos.features.notification.domain.model

import java.time.Instant

/**
 * Append-only audit record of a single notification dispatch attempt.
 *
 * [id] is null before persistence (assigned by the DB via TSID generator).
 * [deviceTokenId] may be null if the token was already removed at dispatch time.
 * [fcmMessageId] is set only for status [NotificationStatus.SENT].
 * [error] is set for [NotificationStatus.FAILED] and [NotificationStatus.TOKEN_INVALIDATED].
 */
data class NotificationLogEntry(
    val id: Long?,
    val tenantId: Long,
    val userId: Long,
    val deviceTokenId: Long?,
    val notificationType: NotificationType,
    val payloadJson: String,
    val status: NotificationStatus,
    val fcmMessageId: String?,
    val error: String?,
    val sentAt: Instant
)
