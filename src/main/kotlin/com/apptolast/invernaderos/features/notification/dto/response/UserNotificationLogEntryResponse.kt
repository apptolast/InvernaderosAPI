package com.apptolast.invernaderos.features.notification.dto.response

import java.time.Instant

data class UserNotificationLogEntryResponse(
    val id: Long,
    val notificationType: String,
    val status: String,
    val payload: Map<String, Any>,
    val fcmMessageId: String?,
    val error: String?,
    val sentAt: Instant
)
