package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller

/**
 * Controller WebSocket para servir datos de negocio enriquecidos.
 *
 * Flujo STOMP:
 * 1. Front conecta a ws://host/ws/greenhouse/status/client
 * 2. Front envia STOMP SEND a /app/status/request
 * 3. Backend consulta PostgreSQL (jerarquia) + TimescaleDB (valores actuales)
 * 4. Responde al usuario que hizo el request con la jerarquia completa enriquecida
 */
@Controller
class GreenhouseStatusWebSocketController(
    private val assembler: GreenhouseStatusAssembler
) {
    private val logger = LoggerFactory.getLogger(GreenhouseStatusWebSocketController::class.java)

    /**
     * Endpoint request-response: el front pide y recibe la jerarquia completa
     * con los valores actuales del hardware embebidos.
     *
     * Destino de envio: /app/status/request
     * Respuesta a: /user/queue/status/response (solo al usuario que pidio)
     */
    @MessageMapping("/status/request")
    @SendToUser("/queue/status/response")
    fun getFullStatus(): GreenhouseStatusResponse {
        logger.info("WebSocket status request received")

        val response = assembler.assembleFullStatus()

        // --- LOG DE VERIFICACIÓN PARA LOS 3 TIPOS ---
        response.tenants.forEach { tenant ->
            tenant.greenhouses.forEach { gh ->
                gh.sectors.forEach { sector ->
                    // 1. Logs de Dispositivos (Sensores/Actuadores)
                    sector.devices.forEach { device ->
                        logger.info("[DEVICE] :  clientName: ${device.clientName}")
                    }

                    // 2. Logs de Configuraciones (Settings)
                    sector.settings.forEach { setting ->
                        logger.info("[SETTING] : clientName: ${setting.clientName}")
                    }

                    // 3. Logs de Alertas
                    sector.alerts.forEach { alert ->
                        logger.info("[ALERT] : clientName: ${alert.clientName}")
                    }
                }
            }
        }

        logger.info("WebSocket status response: {} tenants", response.tenants.size)

        return response
    }
}
