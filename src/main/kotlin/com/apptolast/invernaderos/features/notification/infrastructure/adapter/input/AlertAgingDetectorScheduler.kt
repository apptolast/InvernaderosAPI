package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.domain.port.input.DetectAgingAlertsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDispatchEventPublisherPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduler that periodically scans for aging alerts and publishes [AlertAgingDetectedEvent]s.
 *
 * Runs every 5 minutes (at second 0 of every 5th minute). The cron expression is fixed;
 * to change the interval a redeployment is needed.
 *
 * The use case [DetectAgingAlertsUseCase] handles idempotency internally: alerts already
 * notified within the current activation window are excluded from the returned list.
 *
 * Exceptions are caught globally to ensure the scheduler thread continues running on the
 * next cycle even if a single scan fails.
 */
@Component
class AlertAgingDetectorScheduler(
    private val detectAgingAlertsUseCase: DetectAgingAlertsUseCase,
    private val eventPublisher: NotificationDispatchEventPublisherPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */5 * * * *")
    fun scanAndPublish() {
        try {
            val events = detectAgingAlertsUseCase.detect()
            events.forEach { event ->
                eventPublisher.publishAging(event)
            }
            logger.info(
                "AlertAgingDetectorScheduler: scan complete, detected={} aging alerts",
                events.size
            )
        } catch (ex: Exception) {
            logger.error(
                "AlertAgingDetectorScheduler: scan failed — next run will retry",
                ex
            )
        }
    }
}
