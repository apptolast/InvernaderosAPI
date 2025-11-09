package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.slf4j.LoggerFactory
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * Listener para mensajes de sensores individuales
 *
 * Topic pattern: greenhouse/{greenhouseId}/sensors/{sensorType}
 */
@Component
class SensorDataListener(
    private val messageProcessor: MqttMessageProcessor
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Procesa mensajes de sensores
     * Topic pattern: greenhouse/{greenhouseId}/sensors/{sensorType}
     */
    fun handleSensorData(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String
            val qos = message.headers[MqttHeaders.RECEIVED_QOS] as? Int

            logger.debug("SENSOR message received - Topic: {}, QoS: {}, Payload: {}",
                topic, qos, payload)

            // Parsear el topic: greenhouse/{id}/sensors/{type}
            val parts = topic.split("/")
            if (parts.size >= 4 && parts[0] == "greenhouse" && parts[2] == "sensors") {
                val greenhouseId = parts[1]
                val sensorType = parts[3]

                messageProcessor.processSensorData(greenhouseId, sensorType, payload)
            } else {
                logger.warn("Invalid topic format: {}", topic)
            }

        } catch (e: Exception) {
            logger.error("Error processing SENSOR message: {}", e.message, e)
            throw e  // Re-throw para que el error channel lo maneje
        }
    }
}