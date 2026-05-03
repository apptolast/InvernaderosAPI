package com.apptolast.invernaderos.features.alert.domain.model

import java.time.Instant

data class AlertStateChange(
    val id: Long?,                  // TSID, null until persisted
    val alertId: Long,
    val fromResolved: Boolean,
    val toResolved: Boolean,
    val source: AlertSignalSource,
    val rawValue: String?,          // null if source != MQTT
    val at: Instant,
    val actor: AlertActor = AlertActor.System,
)
