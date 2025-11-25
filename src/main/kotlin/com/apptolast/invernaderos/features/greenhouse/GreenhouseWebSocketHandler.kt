package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.mqtt.service.GreenhouseMessageEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Handler de WebSocket para transmisión en tiempo real de mensajes GREENHOUSE
 *
 * Este componente escucha los eventos de GreenhouseMessageEvent publicados
 * por MqttMessageProcessor y los transmite a los clientes WebSocket suscritos.
 *
 * Arquitectura event-driven:
 * 1. MQTT recibe mensaje → GreenhouseDataListener
 * 2. MqttMessageProcessor procesa y publica GreenhouseMessageEvent
 * 3. GreenhouseWebSocketHandler escucha el evento
 * 4. Broadcast del mensaje a todos los clientes WebSocket suscritos
 *
 * Topics WebSocket:
 * - /topic/greenhouse/messages - Mensajes en tiempo real
 */
@Component
class GreenhouseWebSocketHandler(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(GreenhouseWebSocketHandler::class.java)

    /**
     * Escucha eventos de nuevos mensajes GREENHOUSE y los transmite via WebSocket
     *
     * Este método se ejecuta automáticamente cuando MqttMessageProcessor
     * publica un GreenhouseMessageEvent
     *
     * @param event Evento con el mensaje GREENHOUSE
     */
    @EventListener
    fun handleGreenhouseMessage(event: GreenhouseMessageEvent) {
        try {
            logger.debug("Evento recibido, transmitiendo via WebSocket: {}", event.message.timestamp)

            // Transmitir el mensaje a todos los clientes suscritos al topic
            messagingTemplate.convertAndSend(
                "/topic/greenhouse/messages",
                event.message
            )

            logger.trace("Mensaje transmitido exitosamente via WebSocket")

        } catch (e: Exception) {
            logger.error("Error transmitiendo mensaje via WebSocket", e)
        }
    }

    /**
     * Envía un mensaje específico a un topic
     *
     * Método auxiliar para enviar mensajes programáticamente
     *
     * @param destination Topic de destino (ej: /topic/greenhouse/messages)
     * @param payload Contenido a enviar
     */
    fun sendMessage(destination: String, payload: Any) {
        try {
            messagingTemplate.convertAndSend(destination, payload)
            logger.debug("Mensaje enviado a {}", destination)
        } catch (e: Exception) {
            logger.error("Error enviando mensaje a {}", destination, e)
        }
    }

    /**
     * Envía un mensaje a un usuario específico
     *
     * @param username Nombre del usuario
     * @param destination Topic de destino
     * @param payload Contenido a enviar
     */
    fun sendToUser(username: String, destination: String, payload: Any) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload)
            logger.debug("Mensaje enviado a usuario {} en {}", username, destination)
        } catch (e: Exception) {
            logger.error("Error enviando mensaje a usuario {}", username, e)
        }
    }

    /**
     * Transmite estadísticas actualizadas a los clientes
     *
     * @param statistics Estadísticas a transmitir
     */
    fun broadcastStatistics(statistics: Any) {
        try {
            messagingTemplate.convertAndSend("/topic/greenhouse/statistics", statistics)
            logger.debug("Estadísticas transmitidas via WebSocket")
        } catch (e: Exception) {
            logger.error("Error transmitiendo estadísticas via WebSocket", e)
        }
    }
}
