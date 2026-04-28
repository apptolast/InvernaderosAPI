package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.port.output.AlertEchoPublisherPort
import com.apptolast.invernaderos.mqtt.publisher.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Implements the alert echo by publishing to the same `GREENHOUSE/RESPONSE` topic
 * (and same numeric format) that `MqttCommandPublisherAdapter` uses for SET-/DEV-
 * commands, keeping the bridge contract consistent without coupling alert and
 * command bounded contexts at the domain layer.
 *
 * QoS=1 / retained=false matches the rest of the API→hardware traffic — the message
 * is at-least-once and idempotent (echoing the same value twice is safe).
 */
@Component
class AlertMqttEchoPublisherAdapter(
    private val mqttPublisher: MqttPublisher,
    @Value("\${spring.mqtt.topics.response:GREENHOUSE/RESPONSE}")
    private val mqttResponseTopic: String
) : AlertEchoPublisherPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(code: String, value: Int) {
        val payload = """{"id":"$code","value":$value}"""
        mqttPublisher.publish(mqttResponseTopic, payload, qos = 1)
        logger.debug("Alert echo dispatched topic={} payload={}", mqttResponseTopic, payload)
    }
}
