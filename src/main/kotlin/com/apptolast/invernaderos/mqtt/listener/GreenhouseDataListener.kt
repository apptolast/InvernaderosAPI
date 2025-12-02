package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import com.apptolast.invernaderos.mqtt.service.MqttPublishService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.time.Instant

import com.apptolast.invernaderos.features.greenhouse.toRealDataDto
/**
 * Listener para mensajes de topics GREENHOUSE
 *
 * Soporta dos formatos de topic:
 * 1. Legacy: "GREENHOUSE" (migración DEFAULT tenant)
 * 2. Multi-tenant: "GREENHOUSE/empresaID" (e.g., GREENHOUSE/SARA, GREENHOUSE/001)
 *
 * Procesa mensajes con formato híbrido:
 * - JSON agregado: {"TEMPERATURA INVERNADERO 01":25.3,"HUMEDAD INVERNADERO 01":60.2,...}
 * - Campos individuales: {"empresaID_sensorID_valor": 25.3, ...}
 *
 * Además, envía automáticamente el mensaje recibido de vuelta al broker MQTT
 * (en el topic SYSTEM/RESPONSE) para permitir verificación bidireccional
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

            // Extraer tenant/empresaID del topic path
            // Formato: "GREENHOUSE/empresaID" → empresaID
            // Legacy: "GREENHOUSE" → "DEFAULT"
            val tenantId = when {
                topic.startsWith("GREENHOUSE/") -> topic.substringAfter("GREENHOUSE/").takeWhile { it != '/' }
                topic == "GREENHOUSE" -> "DEFAULT"
                else -> "UNKNOWN"
            }

            logger.info("Processing GREENHOUSE data - Topic: {}, TenantID: {}", topic, tenantId)

            // Procesar el mensaje (guardar en DB, cache, validación tenant, etc.)
            messageProcessor.processGreenhouseData(payload, tenantId)

            // ✅ NUEVO: Enviar automáticamente el mensaje de vuelta al broker MQTT (echo)
            // Esto permite a Jesús y otros sistemas verificar que los datos se reciben correctamente
            try {
                val messageDto = payload.toRealDataDto(
                    timestamp = Instant.now(),
                    greenhouseId = tenantId  // Usar el tenantId extraído del topic
                )

                val published = mqttPublishService.publishGreenhouseData(messageDto)

                if (published) {
                    logger.info("✅ MQTT echo sent successfully - TenantID: {}, Topic: {}", tenantId, topic)
                } else {
                    logger.warn("⚠️ Failed to send MQTT echo - TenantID: {}, Topic: {}", tenantId, topic)
                }
            } catch (echoError: Exception) {
                // No lanzar excepción para que el procesamiento principal no falle
                logger.error("❌ Error sending MQTT echo for tenant {}: {}", tenantId, echoError.message, echoError)
            }

        } catch (e: Exception) {
            logger.error("Error processing GREENHOUSE message: {}", e.message, e)
            throw e  // Re-throw para que el error channel lo maneje
        }
    }
}