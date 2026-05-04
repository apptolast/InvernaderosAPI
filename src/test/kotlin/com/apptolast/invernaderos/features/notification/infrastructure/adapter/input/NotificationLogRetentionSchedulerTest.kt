package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.infrastructure.config.NotificationProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp

class NotificationLogRetentionSchedulerTest {

    private val jdbc = mockk<JdbcTemplate>()
    private val meterRegistry = SimpleMeterRegistry()
    private val scheduler = NotificationLogRetentionScheduler(
        jdbc = jdbc,
        props = NotificationProperties(log = NotificationProperties.LogProps(retentionDays = 90)),
        meterRegistry = meterRegistry
    )

    @Test
    fun `should purge notification logs older than retention window`() {
        every { jdbc.update(any<String>(), any<Timestamp>(), any<Int>()) } returns 3

        scheduler.purge()

        verify(exactly = 1) {
            jdbc.update(
                match { it.contains("DELETE FROM metadata.notification_log") },
                any<Timestamp>(),
                5000
            )
        }
        assertThat(meterRegistry.counter("notification_log_purged_rows").count()).isEqualTo(3.0)
    }

    @Test
    fun `should purge notification logs in batches`() {
        every { jdbc.update(any<String>(), any<Timestamp>(), any<Int>()) } returnsMany listOf(5000, 2)

        scheduler.purge()

        verify(exactly = 2) {
            jdbc.update(
                match { it.contains("WITH stale AS") },
                any<Timestamp>(),
                5000
            )
        }
        assertThat(meterRegistry.counter("notification_log_purged_rows").count()).isEqualTo(5002.0)
    }

    @Test
    fun `should reject unsafe retention window`() {
        val violations = Validation.buildDefaultValidatorFactory()
            .validator
            .validate(NotificationProperties.LogProps(retentionDays = 0))

        assertThat(violations).isNotEmpty
    }
}
