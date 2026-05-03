package com.apptolast.invernaderos.features.notification.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class QuietHoursTest {

    private val utc = ZoneId.of("UTC")
    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `should return false when start and end are null`() {
        val quietHours = QuietHours(start = null, end = null, timezone = utc)

        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T12:00:00Z"))).isFalse()
    }

    @Test
    fun `should return false when start equals end`() {
        val time = LocalTime.of(10, 0)
        val quietHours = QuietHours(start = time, end = time, timezone = utc)

        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T10:00:00Z"))).isFalse()
    }

    @Test
    fun `should return true when within same-day window 09-00 to 17-00 at 12-00`() {
        val quietHours = QuietHours(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            timezone = utc
        )
        // 12:00 UTC falls inside 09:00–17:00
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T12:00:00Z"))).isTrue()
    }

    @Test
    fun `should return false when outside same-day window 09-00 to 17-00 at 18-00`() {
        val quietHours = QuietHours(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            timezone = utc
        )
        // 18:00 UTC is after the 09:00–17:00 window
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T18:00:00Z"))).isFalse()
    }

    @Test
    fun `should return true when within wrap-around window 22-00 to 07-00 at 23-30`() {
        val quietHours = QuietHours(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            timezone = utc
        )
        // 23:30 UTC is inside the wrap-around window (22:00 → midnight → 07:00)
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T23:30:00Z"))).isTrue()
    }

    @Test
    fun `should return true when within wrap-around window 22-00 to 07-00 at 03-00`() {
        val quietHours = QuietHours(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            timezone = utc
        )
        // 03:00 UTC is inside the wrap-around window (after midnight, before 07:00)
        assertThat(quietHours.isWithin(Instant.parse("2026-01-02T03:00:00Z"))).isTrue()
    }

    @Test
    fun `should return false when outside wrap-around 22-00 to 07-00 at 12-00`() {
        val quietHours = QuietHours(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            timezone = utc
        )
        // 12:00 UTC is outside the wrap-around window
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T12:00:00Z"))).isFalse()
    }

    @Test
    fun `should respect user timezone Europe-Madrid`() {
        // Window 22:00–07:00 in Europe/Madrid (UTC+1 in winter)
        // An Instant of 22:00 Madrid = 21:00 UTC → isWithin must return true (uses Madrid TZ)
        val quietHours = QuietHours(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            timezone = madrid
        )
        // 2026-01-01T21:00:00Z = 22:00:00 Europe/Madrid (UTC+1 in January)
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T21:00:00Z"))).isTrue()
        // 2026-01-01T20:00:00Z = 21:00:00 Europe/Madrid → outside the window
        assertThat(quietHours.isWithin(Instant.parse("2026-01-01T20:00:00Z"))).isFalse()
    }
}
