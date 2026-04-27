package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalDecision
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSignalDecisionPort
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AlertSignalDecisionAdapter(
    private val properties: AlertMqttProperties
) : AlertSignalDecisionPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Maps a raw MQTT signal value and current alert state into a decision.
     *
     * Parseability validation: truthy = "true"/"1", falsy = "false"/"0" (case-insensitive).
     * Unparseable values return NO_OP (the use case validates parseability before calling this port,
     * so an unparseable value reaching here is a defensive fallback only).
     *
     * Mapping policy is driven by alert.mqtt.value-true-means:
     *   ACTIVE  (default) → rawValue "true/1" means ACTIVATE, "false/0" means RESOLVE
     *   RESOLVED          → rawValue "true/1" means RESOLVE,   "false/0" means ACTIVATE
     */
    override fun decide(currentAlert: Alert, signal: AlertMqttSignal): AlertSignalDecision {
        val parsedBoolean = parseBoolean(signal.rawValue)
        if (parsedBoolean == null) {
            logger.warn(
                "AlertSignalDecisionAdapter: unparseable rawValue '{}' for code '{}' — returning NO_OP",
                signal.rawValue, signal.code
            )
            return AlertSignalDecision.NO_OP
        }

        val signalMeansActive = when (properties.valueTrueMeans) {
            AlertMqttProperties.ValueTrueMeans.ACTIVE -> parsedBoolean
            AlertMqttProperties.ValueTrueMeans.RESOLVED -> !parsedBoolean
        }

        return when {
            signalMeansActive == !currentAlert.isResolved -> AlertSignalDecision.NO_OP
            signalMeansActive -> AlertSignalDecision.ACTIVATE
            else -> AlertSignalDecision.RESOLVE
        }
    }

    private fun parseBoolean(rawValue: String): Boolean? = when (rawValue.lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> null
    }
}
