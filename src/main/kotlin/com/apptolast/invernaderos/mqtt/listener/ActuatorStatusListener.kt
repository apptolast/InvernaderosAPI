package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class ActuatorStatusListener(
    private val messageProcessor: MqttMessageProcessor
) {

    /**
     * Procesa respuestas de actuadores
     * Topic pattern: greenhouse/{greenhouseId}/actuators/status
     */
    fun handleActuatorStatus(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String

            println("🎛️ [ACTUATOR] Estado recibido:")
            println("   Topic: $topic")
            println("   Payload: $payload")

            val parts = topic.split("/")
            if (parts.size >= 4 && parts[0] == "greenhouse" && parts[2] == "actuators") {
                val greenhouseId = parts[1]
                messageProcessor.processActuatorStatus(greenhouseId, payload)
            }

        } catch (e: Exception) {
            println("❌ Error procesando estado de actuador: ${e.message}")
            e.printStackTrace()
        }
    }
}