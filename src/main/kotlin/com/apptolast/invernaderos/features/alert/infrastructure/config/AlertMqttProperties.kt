package com.apptolast.invernaderos.features.alert.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "alert.mqtt")
data class AlertMqttProperties(
    val valueTrueMeans: ValueTrueMeans = ValueTrueMeans.ACTIVE
) {
    enum class ValueTrueMeans { ACTIVE, RESOLVED }
}
