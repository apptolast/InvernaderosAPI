package com.apptolast.invernaderos.features.notification.domain.model

import java.time.Instant

/**
 * Pure domain event produced by [com.apptolast.invernaderos.features.notification.domain.port.input.DetectAgingAlertsUseCase]
 * when an active alert has been unresolved longer than its severity threshold.
 *
 * This is NOT a Spring ApplicationEvent. The infrastructure adapter that receives the list
 * of these events is responsible for publishing them to the Spring event bus if needed.
 *
 * [ageMinutes] is the number of minutes the alert has been active since [activatedAt].
 * [thresholdMinutes] is the configured threshold that was exceeded to produce this event.
 */
data class AlertAgingDetectedEvent(
    val alertId: Long,
    val alertCode: String,
    val tenantId: Long,
    val severityId: Short,
    val severityLevel: Short,
    val severityName: String,
    val activatedAt: Instant,
    val ageMinutes: Long,
    val thresholdMinutes: Int
)
