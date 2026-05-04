package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.infrastructure.config.NotificationProperties
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class NotificationLogRetentionScheduler(
    @Qualifier("metadataJdbcTemplate") private val jdbc: JdbcTemplate,
    private val props: NotificationProperties,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @SchedulerLock(name = "purge-notification-log", lockAtLeastFor = "PT5M", lockAtMostFor = "PT15M")
    fun purge() {
        val cutoff = Instant.now().minus(props.log.retentionDays.toLong(), ChronoUnit.DAYS)
        val deleted = jdbc.update(
            "DELETE FROM metadata.notification_log WHERE sent_at < ?",
            Timestamp.from(cutoff)
        )
        meterRegistry.counter("notification_log_purged_rows_total").increment(deleted.toDouble())
        log.info("Purged {} notification_log rows older than {}", deleted, cutoff)
    }
}
