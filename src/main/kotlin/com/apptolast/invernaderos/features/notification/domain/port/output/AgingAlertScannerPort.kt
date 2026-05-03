package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import java.time.Instant

/**
 * Driven port for querying active alerts that have exceeded their aging threshold.
 *
 * The adapter executes the necessary SQL/JPQL against the alerts and alert_state_changes
 * tables and returns one [AgingCandidate] per alert that qualifies.
 */
interface AgingAlertScannerPort {
    fun scan(thresholds: AgingThresholdsConfig): List<AgingCandidate>
}

/**
 * An active alert that has been unresolved longer than its configured threshold.
 *
 * [lastActivationAt] is the timestamp of the most recent activation state-change,
 * used instead of the original [Alert.createdAt] to correctly handle alerts that
 * have toggled (resolved then re-activated).
 * [ageMinutes] is the number of minutes elapsed since [lastActivationAt].
 * [thresholdMinutes] is the threshold that was exceeded.
 */
data class AgingCandidate(
    val alert: Alert,
    val lastActivationAt: Instant,
    val ageMinutes: Long,
    val thresholdMinutes: Int,
    val severityName: String
)
