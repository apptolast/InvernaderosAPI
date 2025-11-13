package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.mqtt.service.MqttPublishService
import com.apptolast.invernaderos.service.GreenhouseCacheService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para publicar mensajes al broker MQTT
 *
 * Permite la publicación manual de datos al broker MQTT para verificación
 * y contraste de información con otros sistemas
 */
@RestController
@RequestMapping("/api/mqtt")
@Tag(name = "MQTT", description = "Endpoints para publicación manual de mensajes MQTT")
@CrossOrigin(origins = ["*"])
class MqttPublishController(
    private val mqttPublishService: MqttPublishService,
    private val greenhouseCacheService: GreenhouseCacheService
) {

    /**
     * POST /api/mqtt/publish
     * Publica el último mensaje recibido de un greenhouse al broker MQTT
     *
     * Este endpoint permite enviar manualmente los últimos datos recibidos
     * de vuelta al broker MQTT para verificar que no se pierda información
     */
    @PostMapping("/publish")
    @Operation(
        summary = "Publicar último mensaje recibido",
        description = "Publica el último mensaje recibido al topic GREENHOUSE/RESPONSE. " +
                "Útil para contrastar información y verificar que los datos lleguen correctamente."
    )
    fun publishLatestMessage(
        @Parameter(description = "Topic MQTT de destino (opcional)")
        @RequestParam(required = false)
        topic: String? = null,

        @Parameter(description = "Quality of Service: 0, 1, o 2 (opcional)")
        @RequestParam(required = false)
        qos: Int? = null
    ): ResponseEntity<Map<String, Any>> {

        // Obtener el último mensaje del cache Redis
        val lastMessage = greenhouseCacheService.getLatestMessage() ?: return ResponseEntity.badRequest().body(
            mapOf(
                "success" to false,
                "error" to "No hay mensajes disponibles en la caché"
            )
        )

        // Publicar al broker MQTT
        val published = if (topic != null && qos != null) {
            mqttPublishService.publishGreenhouseData(lastMessage, topic, qos)
        } else {
            mqttPublishService.publishGreenhouseData(lastMessage)
        }

        return if (published) {
            ResponseEntity.ok(
                mapOf<String, Any>(
                    "success" to true,
                    "message" to "Mensaje publicado correctamente al broker MQTT",
                    "greenhouseId" to (lastMessage.greenhouseId ?: "unknown"),
                    "topic" to (topic ?: "GREENHOUSE/RESPONSE"),
                    "qos" to (qos ?: 0),
                    "data" to lastMessage
                )
            )
        } else {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to "Error al publicar mensaje al broker MQTT"
                )
            )
        }
    }

    /**
     * POST /api/mqtt/publish/custom
     * Publica un mensaje personalizado al broker MQTT
     */
    @PostMapping("/publish/custom")
    @Operation(
        summary = "Publicar mensaje personalizado",
        description = "Publica un RealDataDto personalizado al broker MQTT"
    )
    fun publishCustomMessage(
        @Parameter(description = "Datos a publicar en formato RealDataDto")
        @RequestBody
        messageDto: RealDataDto,

        @Parameter(description = "Topic MQTT de destino (opcional)")
        @RequestParam(required = false)
        topic: String? = null,

        @Parameter(description = "Quality of Service: 0, 1, o 2 (opcional)")
        @RequestParam(required = false)
        qos: Int? = null
    ): ResponseEntity<Map<String, Any>> {

        val published = if (topic != null && qos != null) {
            mqttPublishService.publishGreenhouseData(messageDto, topic, qos)
        } else {
            mqttPublishService.publishGreenhouseData(messageDto)
        }

        return if (published) {
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Mensaje personalizado publicado correctamente",
                    "topic" to (topic ?: "GREENHOUSE/RESPONSE"),
                    "qos" to (qos ?: 0),
                    "data" to messageDto
                )
            )
        } else {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to "Error al publicar mensaje personalizado"
                )
            )
        }
    }

    /**
     * POST /api/mqtt/publish/raw
     * Publica un JSON raw al broker MQTT
     */
    @PostMapping("/publish/raw")
    @Operation(
        summary = "Publicar JSON raw",
        description = "Publica un payload JSON directamente al broker MQTT sin validación"
    )
    fun publishRawJson(
        @Parameter(description = "JSON payload a publicar")
        @RequestBody
        jsonPayload: String,

        @Parameter(description = "Topic MQTT de destino (opcional)")
        @RequestParam(required = false)
        topic: String? = null,

        @Parameter(description = "Quality of Service: 0, 1, o 2 (opcional)")
        @RequestParam(required = false)
        qos: Int? = null
    ): ResponseEntity<Map<String, Any>> {

        val published = if (topic != null && qos != null) {
            mqttPublishService.publishRawJson(jsonPayload, topic, qos)
        } else {
            mqttPublishService.publishRawJson(jsonPayload)
        }

        return if (published) {
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "JSON raw publicado correctamente",
                    "topic" to (topic ?: "GREENHOUSE/RESPONSE"),
                    "qos" to (qos ?: 0)
                )
            )
        } else {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to "Error al publicar JSON raw"
                )
            )
        }
    }
}
