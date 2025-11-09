package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.slf4j.LoggerFactory
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * Listener para mensajes del topic GREENHOUSE
 *
 * Procesa mensajes con formato:
 * {"SENSOR_01":1.23,"SENSOR_02":2.23,"SETPOINT_01":0.1,"SETPOINT_02":0.2,"SETPOINT_03":0.3}
 */
@Component
class GreenhouseDataListener(
    private val messageProcessor: MqttMessageProcessor
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Procesa mensajes del topic GREENHOUSE
     */
    fun handleGreenhouseData(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String
            val qos = message.headers[MqttHeaders.RECEIVED_QOS] as? Int

            logger.debug("GREENHOUSE message received - Topic: {}, QoS: {}, Payload: {}",
                topic, qos, payload)

            // TODO: Extraer greenhouseId del topic cuando se migre a "greenhouse/{id}"
            // Por ahora, el topic "GREENHOUSE" no contiene ID, usar default
            val greenhouseId = "001"

            // Procesar el mensaje
            messageProcessor.processGreenhouseData(payload, greenhouseId)

        } catch (e: Exception) {
            logger.error("Error processing GREENHOUSE message: {}", e.message, e)
            throw e  // Re-throw para que el error channel lo maneje
        }
    }
}