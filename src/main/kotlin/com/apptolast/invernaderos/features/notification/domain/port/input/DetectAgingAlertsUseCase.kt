package com.apptolast.invernaderos.features.notification.domain.port.input

import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent

/**
 * Driving port invoked by the scheduler adapter to detect active alerts that have
 * exceeded their aging threshold without being resolved.
 *
 * Returns the list of [AlertAgingDetectedEvent]s that were detected in this run.
 * Idempotency is handled internally: alerts for which a SENT log entry already exists
 * within the current activation window are excluded.
 *
 * The caller (scheduler adapter) is responsible for publishing each event to the
 * Spring application event bus so that the FCM listener can react.
 */
interface DetectAgingAlertsUseCase {
    fun detect(): List<AlertAgingDetectedEvent>
}
