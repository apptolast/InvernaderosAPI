package com.apptolast.invernaderos.features.notification.domain.port.output

/**
 * Driven port for resolving alert severity metadata needed during notification dispatch.
 *
 * Returns null when no severity with the given [severityId] exists in the catalog.
 */
interface AlertSeverityLookupPort {
    fun findById(severityId: Short): NotificationSeveritySnapshot?
}

/**
 * Read-only projection of an alert severity, used exclusively within the notification domain.
 *
 * [notifyPush] mirrors the admin-level flag that suppresses FCM push for this severity globally.
 * When false, the entire dispatch for any notification of this severity must be skipped.
 */
data class NotificationSeveritySnapshot(
    val id: Short,
    val name: String,
    val level: Short,
    val color: String?,
    val notifyPush: Boolean
)
