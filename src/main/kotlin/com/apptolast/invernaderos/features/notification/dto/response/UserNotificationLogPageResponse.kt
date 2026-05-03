package com.apptolast.invernaderos.features.notification.dto.response

data class UserNotificationLogPageResponse(
    val entries: List<UserNotificationLogEntryResponse>,
    val nextCursor: Long?
)
