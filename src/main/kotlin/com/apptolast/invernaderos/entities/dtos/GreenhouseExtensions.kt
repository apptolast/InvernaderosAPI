package com.apptolast.invernaderos.entities.dtos

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

/**
 * Extension functions para convertir entre diferentes representaciones
 * de mensajes GREENHOUSE
 */

private val objectMapper = jacksonObjectMapper()

/**
 * Convierte un JSON string del formato GREENHOUSE a GreenhouseMessageDto
 *
 * Ejemplo de JSON:
 * {
 *   "SENSOR_01": 1.23,
 *   "SENSOR_02": 2.23,
 *   "SETPOINT_01": 0.1,
 *   "SETPOINT_02": 0.2,
 *   "SETPOINT_03": 0.3
 * }
 */
fun String.toGreenhouseMessageDto(
    timestamp: Instant = Instant.now(),
    greenhouseId: String? = null
): GreenhouseMessageDto {
    val jsonNode: JsonNode = objectMapper.readTree(this)

    return GreenhouseMessageDto(
        timestamp = timestamp,
        sensor01 = jsonNode.get("SENSOR_01")?.asDouble(),
        sensor02 = jsonNode.get("SENSOR_02")?.asDouble(),
        setpoint01 = jsonNode.get("SETPOINT_01")?.asDouble(),
        setpoint02 = jsonNode.get("SETPOINT_02")?.asDouble(),
        setpoint03 = jsonNode.get("SETPOINT_03")?.asDouble(),
        greenhouseId = greenhouseId,
        rawPayload = this
    )
}

/**
 * Convierte un Map a GreenhouseMessageDto
 */
fun Map<String, Any>.toGreenhouseMessageDto(
    timestamp: Instant = Instant.now(),
    greenhouseId: String? = null
): GreenhouseMessageDto {
    return GreenhouseMessageDto(
        timestamp = timestamp,
        sensor01 = (this["SENSOR_01"] as? Number)?.toDouble(),
        sensor02 = (this["SENSOR_02"] as? Number)?.toDouble(),
        setpoint01 = (this["SETPOINT_01"] as? Number)?.toDouble(),
        setpoint02 = (this["SETPOINT_02"] as? Number)?.toDouble(),
        setpoint03 = (this["SETPOINT_03"] as? Number)?.toDouble(),
        greenhouseId = greenhouseId,
        rawPayload = objectMapper.writeValueAsString(this)
    )
}

/**
 * Convierte una lista de GreenhouseMessageDto a JSON string
 */
fun List<GreenhouseMessageDto>.toJson(): String {
    return objectMapper.writeValueAsString(this)
}

/**
 * Convierte un GreenhouseMessageDto a JSON string
 */
fun GreenhouseMessageDto.toJson(): String {
    return objectMapper.writeValueAsString(this)
}
