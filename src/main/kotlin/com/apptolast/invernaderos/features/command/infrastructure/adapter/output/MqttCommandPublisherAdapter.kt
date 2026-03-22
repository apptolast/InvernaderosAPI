package com.apptolast.invernaderos.features.command.infrastructure.adapter.output

import com.apptolast.invernaderos.features.command.domain.port.output.CommandPublisherPort
import com.apptolast.invernaderos.mqtt.publisher.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MqttCommandPublisherAdapter(
    private val mqttPublisher: MqttPublisher,
    @Value("\${spring.mqtt.topics.response:GREENHOUSE/RESPONSE}")
    private val mqttResponseTopic: String
) : CommandPublisherPort {

    private val logger = LoggerFactory.getLogger(MqttCommandPublisherAdapter::class.java)

    override fun publish(code: String, value: String) {
        val payload = """{"id":"$code","value":$value}"""
        mqttPublisher.publish(mqttResponseTopic, payload, qos = 1)
        logger.info("Command published to MQTT topic={}: {}", mqttResponseTopic, payload)
    }
}
