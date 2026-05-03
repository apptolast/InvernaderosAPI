package com.apptolast.invernaderos.features.notification.domain.model

enum class NotificationStatus {
    SENT,
    FAILED,
    DROPPED_BY_PREFERENCE,
    DROPPED_BY_QUIET_HOURS,
    DROPPED_BY_DEDUP,
    TOKEN_INVALIDATED
}
