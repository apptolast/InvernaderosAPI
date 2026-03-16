package com.apptolast.invernaderos.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configuracion de WebSocket para consulta de datos de negocio enriquecidos.
 *
 * Utiliza STOMP sobre WebSocket en modo request-response:
 * el front envia un request y el backend responde con la jerarquia completa
 * de negocio (tenants > greenhouses > sectors > devices/settings/alerts)
 * con los valores actuales del hardware embebidos.
 *
 * Endpoint: ws://host/ws/greenhouse/status/client
 *
 * Flujo STOMP:
 * 1. Front conecta a ws://host/ws/greenhouse/status/client
 * 2. Front envia STOMP SEND a /app/status/request
 * 3. Backend responde a /user/queue/status/response
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        logger.info("Configurando Message Broker para WebSocket")

        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        logger.info("Registrando endpoint STOMP: /ws/greenhouse/status/client")

        registry.addEndpoint("/ws/greenhouse/status/client")
            .setAllowedOriginPatterns("*")
            .withSockJS()

        registry.addEndpoint("/ws/greenhouse/status/client")
            .setAllowedOriginPatterns("*")

        logger.info("WebSocket endpoint registrado: ws://host/ws/greenhouse/status/client")
        logger.info("Flujo: SEND /app/status/request -> respuesta en /user/queue/status/response")
    }
}
