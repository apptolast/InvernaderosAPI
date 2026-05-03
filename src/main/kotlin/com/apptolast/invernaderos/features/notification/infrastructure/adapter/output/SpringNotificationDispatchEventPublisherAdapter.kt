package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDispatchEventPublisherPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringNotificationDispatchEventPublisherAdapter(
    private val applicationEventPublisher: ApplicationEventPublisher
) : NotificationDispatchEventPublisherPort {

    override fun publishAging(event: AlertAgingDetectedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
