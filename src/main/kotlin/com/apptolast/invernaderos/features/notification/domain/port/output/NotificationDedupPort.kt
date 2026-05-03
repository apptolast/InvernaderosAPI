package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationType

/**
 * Driven port for deduplication of push notifications within a rolling time window.
 *
 * The adapter implementation uses Redis SET NX EX to track recently-dispatched
 * notifications keyed by (type, alertId, userId).
 *
 * **Fail-open contract**: if the backing store is unavailable, the adapter MUST return
 * true (allow dispatch) rather than silently dropping notifications.
 *
 * @return true if the notification should be dispatched (no duplicate in window);
 *         false if a duplicate was detected and the notification should be skipped.
 */
interface NotificationDedupPort {
    fun shouldDispatch(
        type: NotificationType,
        alertId: Long,
        userId: Long,
        windowSeconds: Long
    ): Boolean
}
