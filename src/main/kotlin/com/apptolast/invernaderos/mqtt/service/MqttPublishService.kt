package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.features.greenhouse.RealDataDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessageChannel
import org.springframework.stereotype.Service

import com.apptolast.invernaderos.features.greenhouse.toJson
/**
 * Servicio para publicar mensajes al broker MQTT
 *
 * Permite enviar datos de vuelta al broker MQTT para verificación y contraste de información
 * entre diferentes sistemas (por ejemplo, con Node-RED o dashboard EMQX)
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
     * Publica un mensaje GreenhouseMessageDto al broker MQTT usando topic y QoS por defecto
     *
     * @param messageDto DTO con los datos a publicar
     * @return true si se envió correctamente, false en caso de error
     */
    fun publishGreenhouseData(messageDto: RealDataDto): Boolean {
        return publishGreenhouseData(messageDto, responseTopic, defaultQos)
    }

    /**
     * Publica un mensaje GreenhouseMessageDto al broker MQTT
     *
     * @param messageDto DTO con los datos a publicar
     * @param topic Topic MQTT de destino
     * @param qos Quality of Service (0, 1, o 2)
     * @return true si se envió correctamente, false en caso de error
     */
    fun publishGreenhouseData(
        messageDto: RealDataDto,
        topic: String,
        qos: Int
    ): Boolean {
        return try {
            // Preparar payload según el topic
            val payload = if (topic.contains("GREENHOUSE/MOBILE", ignoreCase = true)) {
                // Para topics GREENHOUSE/MOBILE, usar datos originales
                messageDto
            } else {
                messageDto
            }

            // Publicar el mensaje
            publishMessage(payload, topic, qos)
        } catch (e: Exception) {
            logger.error("Error publishing MQTT message: {}", e.message, e)
            false
        }
    }

    /**
     * Función auxiliar para construir y enviar un mensaje MQTT
     *
     * @param messageDto DTO con los datos a publicar
     * @param topic Topic MQTT de destino
     * @param qos Quality of Service
     * @return true si se envió correctamente, false en caso contrario
     */
    private fun publishMessage(
        messageDto: RealDataDto,
        topic: String,
        qos: Int
    ): Boolean {
        // Convertir DTO a JSON
        val jsonPayload = messageDto.toJson()

        logger.debug("Publishing MQTT message - Topic: {}, QoS: {}, Payload: {}",
            topic, qos, jsonPayload)

        // Crear mensaje Spring Integration con headers MQTT
        val message = MessageBuilder
            .withPayload(jsonPayload)
            .setHeader(MqttHeaders.TOPIC, topic)
            .setHeader(MqttHeaders.QOS, qos)
            .setHeader(MqttHeaders.RETAINED, false)
            .build()

        // Enviar al canal outbound (el MessageHandler lo enviará al broker)
        val sent = mqttOutboundChannel.send(message)

        if (sent) {
            logger.info("MQTT message published successfully - Topic: {}, GreenhouseId: {}",
                topic, messageDto.greenhouseId)
        } else {
            logger.warn("Failed to send MQTT message to channel - Topic: {}", topic)
        }

        return sent
    }

    /**
     * Publica un payload JSON directamente al broker MQTT usando topic y QoS por defecto
     *
     * @param jsonPayload JSON string a publicar
     * @return true si se envió correctamente, false en caso de error
     */
    fun publishRawJson(jsonPayload: String): Boolean {
        return publishRawJson(jsonPayload, responseTopic, defaultQos)
    }

    /**
     * Publica un payload JSON directamente al broker MQTT
     *
     * @param jsonPayload JSON string a publicar
     * @param topic Topic MQTT de destino
     * @param qos Quality of Service
     * @return true si se envió correctamente, false en caso de error
     */
    fun publishRawJson(
        jsonPayload: String,
        topic: String,
        qos: Int
    ): Boolean {
        return try {
            logger.debug("Publishing raw MQTT message - Topic: {}, QoS: {}, Payload: {}",
                topic, qos, jsonPayload)

            val message = MessageBuilder
                .withPayload(jsonPayload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .setHeader(MqttHeaders.RETAINED, false)
                .build()

            val sent = mqttOutboundChannel.send(message)

            if (sent) {
                logger.info("Raw MQTT message published successfully - Topic: {}", topic)
            } else {
                logger.warn("Failed to send raw MQTT message to channel - Topic: {}", topic)
            }

            sent
        } catch (e: Exception) {
            logger.error("Error publishing raw MQTT message: {}", e.message, e)
            false
        }
    }
}
