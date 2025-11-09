package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.entities.dtos.toGreenhouseMessageDto
import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import com.apptolast.invernaderos.mqtt.service.MqttPublishService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Listener para mensajes del topic GREENHOUSE
 *
 * Procesa mensajes con formato:
 * {"SENSOR_01":1.23,"SENSOR_02":2.23,"SETPOINT_01":0.1,"SETPOINT_02":0.2,"SETPOINT_03":0.3}
 *
 * Además, envía automáticamente el mensaje recibido de vuelta al broker MQTT
 * (en el topic GREENHOUSE/RESPONSE) para permitir verificación bidireccional
 */
@Component
class GreenhouseDataListener(
    private val messageProcessor: MqttMessageProcessor,
    @Lazy private val mqttPublishService: MqttPublishService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Procesa mensajes del topic GREENHOUSE y los envía de vuelta automáticamente (echo)
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

            // Procesar el mensaje (guardar en DB, cache, etc.)
            messageProcessor.processGreenhouseData(payload, greenhouseId)

            // ✅ NUEVO: Enviar automáticamente el mensaje de vuelta al broker MQTT (echo)
            // Esto permite a Jesús y otros sistemas verificar que los datos se reciben correctamente
            try {
                val messageDto = payload.toGreenhouseMessageDto(
                    timestamp = Instant.now(),
                    greenhouseId = greenhouseId
                )

                val published = mqttPublishService.publishGreenhouseData(messageDto)

                if (published) {
                    logger.info("✅ MQTT echo sent successfully - GreenhouseId: {}", greenhouseId)
                } else {
                    logger.warn("⚠️ Failed to send MQTT echo - GreenhouseId: {}", greenhouseId)
                }
            } catch (echoError: Exception) {
                // No lanzar excepción para que el procesamiento principal no falle
                logger.error("❌ Error sending MQTT echo: {}", echoError.message, echoError)
            }

        } catch (e: Exception) {
            logger.error("Error processing GREENHOUSE message: {}", e.message, e)
            throw e  // Re-throw para que el error channel lo maneje
        }
    }
}