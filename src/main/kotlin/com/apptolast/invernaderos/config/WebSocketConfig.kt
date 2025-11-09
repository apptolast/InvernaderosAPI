package com.apptolast.invernaderos.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configuración de WebSocket para transmisión en tiempo real de mensajes GREENHOUSE
 *
 * Utiliza STOMP (Simple Text Oriented Messaging Protocol) sobre WebSocket
 * para permitir a los clientes suscribirse a topics y recibir mensajes en tiempo real.
 *
 * Endpoints:
 * - WebSocket: ws://host/ws/greenhouse
 * - SockJS fallback: http://host/ws/greenhouse (para navegadores sin WebSocket)
 *
 * Topics STOMP disponibles:
 * - /topic/greenhouse/messages - Mensajes nuevos del topic GREENHOUSE
 * - /topic/greenhouse/statistics - Actualizaciones de estadísticas
 *
 * Ejemplo de uso desde JavaScript:
 * ```javascript
 * const socket = new SockJS('http://localhost:8080/ws/greenhouse');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, function() {
 *   stompClient.subscribe('/topic/greenhouse/messages', function(message) {
 *     console.log('Nuevo mensaje:', JSON.parse(message.body));
 *   });
 * });
 * ```
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    /**
     * Configura el message broker
     *
     * - /topic: prefix para topics de broadcast (uno a muchos)
     * - /app: prefix para mensajes dirigidos a la aplicación
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        logger.info("Configurando Message Broker para WebSocket")

        // Habilitar un simple message broker en memoria
        registry.enableSimpleBroker("/topic", "/queue")

        // Prefix para mensajes destinados a métodos anotados con @MessageMapping
        registry.setApplicationDestinationPrefixes("/app")

        // Prefix para enviar mensajes a usuarios específicos
        registry.setUserDestinationPrefix("/user")
    }

    /**
     * Registra los endpoints de WebSocket
     *
     * - Endpoint principal: /ws/greenhouse
     * - SockJS habilitado para compatibilidad con navegadores antiguos
     * - CORS permitido desde cualquier origen (ajustar en producción)
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        logger.info("Registrando endpoints STOMP")

        // Endpoint principal de WebSocket
        registry.addEndpoint("/ws/greenhouse")
            .setAllowedOriginPatterns("*") // En producción, especificar orígenes permitidos
            .withSockJS() // Habilitar SockJS como fallback

        // Endpoint adicional sin SockJS para clientes nativos WebSocket
        registry.addEndpoint("/ws/greenhouse-native")
            .setAllowedOriginPatterns("*")

        logger.info("WebSocket endpoints registrados:")
        logger.info("  - ws://host/ws/greenhouse (con SockJS)")
        logger.info("  - ws://host/ws/greenhouse-native (nativo)")
        logger.info("Topics disponibles:")
        logger.info("  - /topic/greenhouse/messages")
        logger.info("  - /topic/greenhouse/statistics")
    }
}
