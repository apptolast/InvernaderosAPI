package com.apptolast.invernaderos.mqtt.publisher

import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class MqttPublisher(
    private val mqttOutboundChannel: MessageChannel
) {

    /**
     * Publica un mensaje a un topic específico
     */
    fun publish(topic: String, payload: String, qos: Int = 0, retained: Boolean = false) {
        val message = MessageBuilder.withPayload(payload)
            .setHeader(MqttHeaders.TOPIC, topic)
            .setHeader(MqttHeaders.QOS, qos)
            .setHeader(MqttHeaders.RETAINED, retained)
            .build()

        mqttOutboundChannel.send(message)
        println("📤 Mensaje publicado a MQTT:")
        println("   Topic: $topic")
        println("   Payload: $payload")
        println("   QoS: $qos")
    }

    /**
     * Envía comando a un actuador
     */
    fun sendActuatorCommand(
        greenhouseId: String,
        actuatorId: String,
        command: String,
        value: Any
    ) {
        val topic = "greenhouse/$greenhouseId/actuators/$actuatorId/command"
        val payload = """{"command":"$command","value":$value,"timestamp":"${java.time.Instant.now()}"}"""
        publish(topic, payload, qos = 1)
    }

    /**
     * Publica una alerta
     */
    fun publishAlert(
        greenhouseId: String,
        alertType: String,
        message: String,
        severity: String
    ) {
        val topic = "greenhouse/$greenhouseId/alerts/$alertType"
        val payload = """{"message":"$message","severity":"$severity","timestamp":"${java.time.Instant.now()}"}"""
        publish(topic, payload, qos = 2)
    }
}