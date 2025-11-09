package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class GreenhouseDataListener(
    private val messageProcessor: MqttMessageProcessor
) {

    /**
     * Procesa mensajes del topic GREENHOUSE
     * Formato: {"SENSOR_01":1.23,"SENSOR_02":0,"SETPOINT_01":5.67,"SETPOINT_02":0}
     */
    fun handleGreenhouseData(message: Message<*>) {
        try {
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: return
            val payload = message.payload as String
            val qos = message.headers[MqttHeaders.RECEIVED_QOS] as? Int

            println("📥 [GREENHOUSE] Mensaje recibido:")
            println("   Topic: $topic")
            println("   QoS: $qos")
            println("   Payload: $payload")

            // Procesar el mensaje
            messageProcessor.processGreenhouseData(payload)

        } catch (e: Exception) {
            println("❌ Error procesando mensaje de greenhouse: ${e.message}")
            e.printStackTrace()
        }
    }
}