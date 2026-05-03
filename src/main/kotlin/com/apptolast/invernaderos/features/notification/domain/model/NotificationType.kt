package com.apptolast.invernaderos.features.notification.domain.model

enum class NotificationType(
    val category: NotificationCategory,
    val defaultChannelId: String
) {
    ALERT_ACTIVATED(NotificationCategory.ALERTS, "alerts_default"),
    ALERT_RESOLVED(NotificationCategory.ALERTS, "alerts_resolved"),
    ALERT_AGING(NotificationCategory.ALERTS, "alerts_aging")
}
