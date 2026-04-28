package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.port.output.AlertEchoPublisherPort
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties
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
 * `fallbackExecution` is left at its default (false): if the event is published
 * outside an active transaction (e.g. from a future batch job that does not open
 * one), the listener silently drops it. This is intentional — emitting an echo
 * without a committed DB write would lie to downstream consumers.
 *
 * Loop-prevention layers (defense in depth):
 *  - L1: API does not subscribe to GREENHOUSE/RESPONSE (see [com.apptolast.invernaderos.config.MqttConfig.mqttInbound]).
 *  - L2 (echo-loop only): [com.apptolast.invernaderos.features.alert.application.usecase.ApplyAlertMqttSignalUseCaseImpl]
 *        returns NoTransitionRequired before publishing the event when an incoming
 *        MQTT signal already matches the alert state. This breaks self-echo loops
 *        in one round; it does not protect against an unrelated MQTT publisher
 *        toggling values on RESPONSE.
 *  - L3: this listener short-circuits when fromResolved == toResolved.
 *  - L4: any [AlertEchoPublisherPort] exception is swallowed and logged. Spring's
 *        SimpleApplicationEventMulticaster also absorbs it for AFTER_COMMIT
 *        listeners; the explicit catch is belt-and-suspenders so future authors do
 *        not depend implicitly on Spring's error-handling policy.
 *  - L5: [com.apptolast.invernaderos.mqtt.MqttSubscriptionGuardTest] fails the
 *        build if anyone adds RESPONSE to MqttConfig.mqttInbound() topics or to a
 *        non-publish key in application.yaml.
 *  - L6: runtime kill-switch alert.mqtt.echo.enabled (default true).
 *
 * Concurrency note: the alerts table has no @Version. Two simultaneous REST
 * /resolve on the same alert can both pass the use-case guard, both write
 * AlertStateChange(API), and trigger two echoes. The DB end state is idempotent
 * and the receiver is expected to be idempotent too, so we accept the duplicate
 * over the cost of optimistic locking. Document this here so the next reader
 * does not add @Version without understanding the trade-off.
 */
@Component
class AlertStateChangedMqttEchoListener(
    private val echoPublisher: AlertEchoPublisherPort,
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
            echoPublisher.publish(event.alert.code, value)
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
