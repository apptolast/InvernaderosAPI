package com.apptolast.invernaderos.features.notification.domain.error

sealed interface NotificationError {
    val message: String

    data class UserNotFound(val userId: Long) : NotificationError {
        override val message: String
            get() = "User $userId not found"
    }

    data class InvalidPreferences(val detail: String) : NotificationError {
        override val message: String
            get() = "Invalid notification preferences: $detail"
    }

    data class AlertNotFound(val alertId: Long) : NotificationError {
        override val message: String
            get() = "Alert $alertId not found"
    }

    data class RenderingFailed(val detail: String) : NotificationError {
        override val message: String
            get() = "Failed to render notification content: $detail"
    }

    data class DispatchPartiallyFailed(val sent: Int, val failed: Int) : NotificationError {
        override val message: String
            get() = "Notification dispatch completed with failures: sent=$sent, failed=$failed"
    }
}
