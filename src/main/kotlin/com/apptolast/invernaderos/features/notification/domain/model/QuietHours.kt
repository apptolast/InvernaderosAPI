package com.apptolast.invernaderos.features.notification.domain.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Represents a user's quiet-hours window.
 *
 * If both [start] and [end] are null, quiet hours are disabled and [isWithin] always returns false.
 * If [start] == [end], the window is considered zero-length and [isWithin] always returns false.
 * Supports wrap-around midnight (e.g. start=22:00, end=07:00).
 */
data class QuietHours(
    val start: LocalTime?,
    val end: LocalTime?,
    val timezone: ZoneId
) {
    /**
     * Returns true if [instant] falls inside the configured quiet-hours window.
     * Converts [instant] to the user's [timezone] before comparing.
     */
    fun isWithin(instant: Instant): Boolean {
        if (start == null || end == null) return false
        if (start == end) return false

        val userLocalTime = instant.atZone(timezone).toLocalTime()

        return if (start.isBefore(end)) {
            // Normal window: e.g. 08:00 – 12:00
            !userLocalTime.isBefore(start) && userLocalTime.isBefore(end)
        } else {
            // Wrap-around midnight: e.g. 22:00 – 07:00
            !userLocalTime.isBefore(start) || userLocalTime.isBefore(end)
        }
    }
}
