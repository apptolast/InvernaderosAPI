package com.apptolast.invernaderos.features.alert.domain.model

data class AlertMqttSignal(
    val code: String,      // e.g. "ALT-00010"
    val rawValue: String   // e.g. "1" or "0" — already converted by the listener
)
