package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class SensorDataListener(
    private val messageProcessor: MqttMessageProcessor
) {

    /**
     * Procesa mensajes de sensores
     * Topic pattern: greenhouse/{greenhouseId}/sensors/{sensorType}
     */
    fun handleSensorData(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String
            val qos = message.headers[MqttHeaders.RECEIVED_QOS] as? Int

            println("📥 [SENSOR] Mensaje recibido:")
            println("   Topic: $topic")
            println("   QoS: $qos")
            println("   Payload: $payload")

            // Parsear el topic: greenhouse/{id}/sensors/{type}
            val parts = topic.split("/")
            if (parts.size >= 4 && parts[0] == "greenhouse" && parts[2] == "sensors") {
                val greenhouseId = parts[1]
                val sensorType = parts[3]

                messageProcessor.processSensorData(greenhouseId, sensorType, payload)
            } else {
                println("⚠️ Formato de topic inválido: $topic")
            }

        } catch (e: Exception) {
            println("❌ Error procesando mensaje de sensor: ${e.message}")
            e.printStackTrace()
        }
    }
}