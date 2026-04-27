package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class AlertStateChangedEventPublisherAdapter(
    private val applicationEventPublisher: ApplicationEventPublisher
) : AlertStateChangedEventPublisherPort {

    override fun publish(alert: Alert, change: AlertStateChange) {
        applicationEventPublisher.publishEvent(AlertStateChangedEvent(alert, change))
    }
}
