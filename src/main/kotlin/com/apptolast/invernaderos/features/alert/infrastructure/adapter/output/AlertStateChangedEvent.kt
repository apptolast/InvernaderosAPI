package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange

/**
 * Spring ApplicationEvent payload published whenever an Alert transitions its is_resolved state.
 * Future WebSocket subscribers can listen for this event to push real-time updates to clients.
 */
data class AlertStateChangedEvent(
    val alert: Alert,
    val change: AlertStateChange
)
