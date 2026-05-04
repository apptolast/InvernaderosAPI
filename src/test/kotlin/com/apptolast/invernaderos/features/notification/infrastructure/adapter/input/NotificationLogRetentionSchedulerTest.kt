package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.infrastructure.config.NotificationProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        every { jdbc.update(any<String>(), any<Timestamp>()) } returns 3

        scheduler.purge()

        verify(exactly = 1) {
            jdbc.update(
                "DELETE FROM metadata.notification_log WHERE sent_at < ?",
                any<Timestamp>()
            )
        }
        assertThat(meterRegistry.counter("notification_log_purged_rows_total").count()).isEqualTo(3.0)
    }
}
