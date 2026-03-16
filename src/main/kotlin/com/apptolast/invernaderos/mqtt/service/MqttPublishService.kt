package com.apptolast.invernaderos.mqtt.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessageChannel
import org.springframework.stereotype.Service

/**
 * Servicio para publicar mensajes al broker MQTT.
 * Permite enviar payloads JSON al broker para verificacion y testing.
 */
@Service
class MqttPublishService(
    private val mqttOutboundChannel: MessageChannel,

    @Value("\${spring.mqtt.topics.response:SYSTEM/RESPONSE}")
    private val responseTopic: String = "SYSTEM/RESPONSE",

    @Value("\${spring.mqtt.qos.default:0}")
    private val defaultQos: Int = 0
) {

    private val logger = LoggerFactory.getLogger(MqttPublishService::class.java)

    /**
     * Publica un payload JSON al broker MQTT usando topic y QoS por defecto.
     */
    fun publishRawJson(jsonPayload: String): Boolean {
        return publishRawJson(jsonPayload, responseTopic, defaultQos)
    }

    /**
     * Publica un payload JSON al broker MQTT.
     */
    fun publishRawJson(
        jsonPayload: String,
        topic: String,
        qos: Int
    ): Boolean {
        return try {
            logger.debug("Publishing MQTT message - Topic: {}, QoS: {}", topic, qos)

            val message = MessageBuilder
                .withPayload(jsonPayload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .setHeader(MqttHeaders.RETAINED, false)
                .build()

            val sent = mqttOutboundChannel.send(message)

            if (sent) {
                logger.info("MQTT message published successfully - Topic: {}", topic)
            } else {
                logger.warn("Failed to send MQTT message - Topic: {}", topic)
            }

            sent
        } catch (e: Exception) {
            logger.error("Error publishing MQTT message: {}", e.message, e)
            false
        }
    }
}
