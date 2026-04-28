package com.apptolast.invernaderos.features.alert.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "alert.mqtt")
data class AlertMqttProperties(
    val valueTrueMeans: ValueTrueMeans = ValueTrueMeans.ACTIVE,
    val echo: Echo = Echo()
) {
    enum class ValueTrueMeans { ACTIVE, RESOLVED }

    /**
     * Runtime kill-switch for the MQTT echo of alert state changes.
     * Disable in production (ALERT_MQTT_ECHO_ENABLED=false) to short-circuit
     * AlertStateChangedMqttEchoListener without redeploying.
     */
    data class Echo(val enabled: Boolean = true)
}
