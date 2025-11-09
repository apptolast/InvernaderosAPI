package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.slf4j.LoggerFactory
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * Listener para mensajes de estado de actuadores
 *
 * Topic pattern: greenhouse/{greenhouseId}/actuators/status
 */
@Component
class ActuatorStatusListener(
    private val messageProcessor: MqttMessageProcessor
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Procesa respuestas de actuadores
     * Topic pattern: greenhouse/{greenhouseId}/actuators/status
     */
    fun handleActuatorStatus(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String

            logger.debug("ACTUATOR status received - Topic: {}, Payload: {}",
                topic, payload)

            val parts = topic.split("/")
            if (parts.size >= 4 && parts[0] == "greenhouse" && parts[2] == "actuators") {
                val greenhouseId = parts[1]
                messageProcessor.processActuatorStatus(greenhouseId, payload)
            } else {
                logger.warn("Invalid topic format for actuator status: {}", topic)
            }

        } catch (e: Exception) {
            logger.error("Error processing ACTUATOR status: {}", e.message, e)
            throw e  // Re-throw para que el error channel lo maneje
        }
    }
}