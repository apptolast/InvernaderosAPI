package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties
import com.apptolast.invernaderos.features.command.domain.port.output.CommandPublisherPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listens for [AlertStateChangedEvent] and echoes the new state to MQTT
 * (`GREENHOUSE/RESPONSE`) so external subscribers — typically the Node-RED bridge —
 * stay in sync with the database.
 *
 * Phase = AFTER_COMMIT: the echo is published only after the metadata transaction
 * commits successfully. If the transaction rolls back, no MQTT message is emitted.
 *
 * Loop-prevention layers (defense in depth):
 *  - L1: API does not subscribe to GREENHOUSE/RESPONSE (see MqttConfig.mqttInbound).
 *  - L2: ApplyAlertMqttSignalUseCaseImpl returns NoTransitionRequired before publishing
 *        the event when the incoming MQTT signal already matches the alert state.
 *  - L3: this listener short-circuits when fromResolved == toResolved.
 *  - L4: any MqttPublisher exception is swallowed and logged — never propagates.
 *  - L5: kill-switch via alert.mqtt.echo.enabled (default true).
 */
@Component
class AlertStateChangedMqttEchoListener(
    private val commandPublisher: CommandPublisherPort,
    private val properties: AlertMqttProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAlertStateChanged(event: AlertStateChangedEvent) {
        if (!properties.echo.enabled) {
            logger.debug("Echo disabled (alert.mqtt.echo.enabled=false), skipping code={}", event.alert.code)
            return
        }

        if (event.change.fromResolved == event.change.toResolved) {
            logger.warn(
                "Echo received non-transition event for code={} (from={} to={}) — defensive skip",
                event.alert.code, event.change.fromResolved, event.change.toResolved
            )
            return
        }

        val value = encodeValue(event.alert.isResolved, properties.valueTrueMeans)

        try {
            commandPublisher.publish(event.alert.code, value.toString())
            logger.info(
                "Echo published code={} source={} isResolved={} value={}",
                event.alert.code, event.change.source, event.alert.isResolved, value
            )
        } catch (ex: Exception) {
            logger.error(
                "Echo failed for code={} source={} value={} — DB state already committed, MQTT publish lost",
                event.alert.code, event.change.source, value, ex
            )
        }
    }

    /**
     * Inverse of [com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertSignalDecisionAdapter]:
     * given the alert's new resolved state and the configured mapping policy,
     * encode the numeric value the hardware bridge expects.
     *
     * `targetActive = !isResolved`
     *  - mode ACTIVE   → value=1 means ACTIVE,   value=0 means RESOLVED
     *  - mode RESOLVED → value=1 means RESOLVED, value=0 means ACTIVE
     */
    private fun encodeValue(
        isResolved: Boolean,
        mode: AlertMqttProperties.ValueTrueMeans
    ): Int {
        val targetActive = !isResolved
        return when (mode) {
            AlertMqttProperties.ValueTrueMeans.ACTIVE -> if (targetActive) 1 else 0
            AlertMqttProperties.ValueTrueMeans.RESOLVED -> if (targetActive) 0 else 1
        }
    }
}
