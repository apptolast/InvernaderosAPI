package com.apptolast.invernaderos.features.mqtt

import com.apptolast.invernaderos.mqtt.service.MqttPublishService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para publicar mensajes al broker MQTT.
 * Permite la publicacion manual de payloads JSON para verificacion y testing.
 */
@RestController
@RequestMapping("/api/v1/mqtt")
@Tag(name = "MQTT", description = "Endpoints para publicacion manual de mensajes MQTT")
@CrossOrigin(origins = ["*"])
class MqttPublishController(
    private val mqttPublishService: MqttPublishService
) {

    @PostMapping("/publish/raw")
    @Operation(
        summary = "Publicar JSON raw",
        description = "Publica un payload JSON directamente al broker MQTT"
    )
    fun publishRawJson(
        @Parameter(description = "JSON payload a publicar")
        @RequestBody
        jsonPayload: String,

        @Parameter(description = "Topic MQTT de destino")
        @RequestParam(required = false)
        topic: String? = null,

        @Parameter(description = "Quality of Service: 0, 1, o 2")
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
                    "message" to "JSON publicado correctamente",
                    "topic" to (topic ?: "GREENHOUSE/RESPONSE"),
                    "qos" to (qos ?: 0)
                )
            )
        } else {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to "Error al publicar JSON al broker MQTT"
                )
            )
        }
    }
}
