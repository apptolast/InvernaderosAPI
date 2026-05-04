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
    @SchedulerLock(name = "purge-notification-log", lockAtLeastFor = "PT5M", lockAtMostFor = "PT2H")
    fun purge() {
        val cutoff = Instant.now().minus(props.log.retentionDays.toLong(), ChronoUnit.DAYS)
        var deletedTotal = 0L
        do {
            val deleted = jdbc.update(
                """
                WITH stale AS (
                    SELECT id
                    FROM metadata.notification_log
                    WHERE sent_at < ?
                    ORDER BY sent_at, id
                    LIMIT ?
                )
                DELETE FROM metadata.notification_log nl
                USING stale
                WHERE nl.id = stale.id
                """.trimIndent(),
                Timestamp.from(cutoff),
                DELETE_BATCH_SIZE
            )
            deletedTotal += deleted
        } while (deleted == DELETE_BATCH_SIZE)

        meterRegistry.counter("notification_log_purged_rows").increment(deletedTotal.toDouble())
        log.info("Purged {} notification_log rows older than {}", deletedTotal, cutoff)
    }

    private companion object {
        const val DELETE_BATCH_SIZE = 5_000
    }
}
