package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent

/**
 * Driven port for publishing domain events produced by the notification module.
 *
 * The adapter implementation delegates to Spring's ApplicationEventPublisher
 * so that infrastructure listeners (e.g. FcmListener) can react to the event
 * after the use-case transaction commits.
 */
interface NotificationDispatchEventPublisherPort {
    fun publishAging(event: AlertAgingDetectedEvent)
}
