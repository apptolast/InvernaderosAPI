package com.apptolast.invernaderos.features.notification.domain.model

import java.time.Duration

/**
 * Maps each alert severity level (Short) to the [Duration] after which an unresolved
 * alert should trigger an ALERT_AGING notification.
 *
 * A null return from [thresholdFor] means no aging notification is configured for that severity.
 *
 * Example thresholds:
 *   - level 4 (CRITICAL) → PT30M
 *   - level 3 (ERROR)    → PT2H
 *   - level 2 (WARNING)  → PT8H
 *   - level 1 (INFO)     → not present (no aging)
 */
@JvmInline
value class AgingThresholdsConfig(val thresholds: Map<Short, Duration>) {

    /** Returns the configured aging threshold for [level], or null if aging is disabled for that level. */
    fun thresholdFor(level: Short): Duration? = thresholds[level]

    /** Returns the levels that have an aging threshold configured. */
    fun configuredLevels(): Set<Short> = thresholds.keys
}
