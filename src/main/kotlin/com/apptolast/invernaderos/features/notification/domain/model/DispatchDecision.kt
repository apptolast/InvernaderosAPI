package com.apptolast.invernaderos.features.notification.domain.model

/**
 * The outcome of evaluating all per-user filters before sending a notification.
 *
 * [Send] means the notification should proceed to FCM dispatch.
 * [Drop] means the notification should be skipped and logged with the associated [DropReason].
 */
sealed interface DispatchDecision {
    data class Send(val content: NotificationContent) : DispatchDecision

    data class Drop(val reason: DropReason) : DispatchDecision
}

/**
 * Reason why a notification was discarded before reaching FCM.
 * Each value maps to a corresponding [NotificationStatus] for the audit log.
 */
enum class DropReason {
    CATEGORY_DISABLED,
    BELOW_MIN_SEVERITY,
    IN_QUIET_HOURS,
    DEDUP_HIT;

    fun toNotificationStatus(): NotificationStatus = when (this) {
        CATEGORY_DISABLED -> NotificationStatus.DROPPED_BY_PREFERENCE
        BELOW_MIN_SEVERITY -> NotificationStatus.DROPPED_BY_PREFERENCE
        IN_QUIET_HOURS -> NotificationStatus.DROPPED_BY_QUIET_HOURS
        DEDUP_HIT -> NotificationStatus.DROPPED_BY_DEDUP
    }
}
