package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange

interface AlertStateChangedEventPublisherPort {
    fun publish(alert: Alert, change: AlertStateChange)
}
