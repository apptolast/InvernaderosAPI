package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.DeviceStatusProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * Listener para mensajes del topic GREENHOUSE/STATUS
 *
 * Formato de mensaje MQTT: {"id":"SET-00036","value":15}
 * - SET-XXXXX corresponde a metadata.settings.code
 * - DEV-XXXXX corresponde a metadata.devices.code
 *
 * Cada mensaje contiene un solo par id/value.
 * Se reciben ~78 mensajes por segundo (uno por cada device/setting).
 */
@Component
class DeviceStatusListener(
    private val deviceStatusProcessor: DeviceStatusProcessor,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Procesa un mensaje individual de GREENHOUSE/STATUS
     */
    fun handleDeviceStatus(message: Message<*>) {
        try {
            val payload = message.payload as String
            val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: ""

            logger.trace("GREENHOUSE/STATUS message received - Topic: {}, Payload: {}", topic, payload)

            val jsonNode = objectMapper.readTree(payload)
            val code = jsonNode.get("id")?.asText()
            val valueNode = jsonNode.get("value")

            if (code == null || valueNode == null) {
                logger.warn("Invalid GREENHOUSE/STATUS message - missing 'id' or 'value': {}", payload)
                return
            }

            // Convertir el valor a String preservando el tipo original
            val value = when {
                valueNode.isBoolean -> valueNode.asBoolean().toString()
                valueNode.isNumber -> valueNode.asText()
                valueNode.isTextual -> valueNode.asText()
                else -> valueNode.toString()
            }

            deviceStatusProcessor.processStatusUpdate(code, value)

        } catch (e: Exception) {
            logger.error("Error processing GREENHOUSE/STATUS message: {}", e.message, e)
        }
    }
}
