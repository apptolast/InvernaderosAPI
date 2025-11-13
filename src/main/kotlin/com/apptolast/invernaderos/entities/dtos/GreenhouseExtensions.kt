package com.apptolast.invernaderos.entities.dtos

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

/**
 * Extension functions para convertir entre diferentes representaciones
 * de mensajes GREENHOUSE
 */

private val objectMapper = jacksonObjectMapper().apply {
    // Registrar JavaTimeModule para soporte de Instant, LocalDateTime, etc.
    registerModule(JavaTimeModule())
    // Desactivar escritura de fechas como timestamps (usar ISO-8601)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}

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
 * Handle Real Data of the sensors
 * Exmaple :
 * {
 * "TEMPERATURA INVERNADERO 01": 6483.8,
 * "HUMEDAD INVERNADERO 01": 6528.7,
 * "TEMPERATURA INVERNADERO 02": 11.9,
 * "HUMEDAD INVERNADERO 02": 95.8,
 * "TEMPERATURA INVERNADERO 03": 6533.6,
 * "HUMEDAD INVERNADERO 03": 0,
 * "INVERNADERO_01_SECTOR_01": 0,
 * "INVERNADERO_01_SECTOR_02": 0,
 * "INVERNADERO_01_SECTOR_03": 0,
 * "INVERNADERO_01_SECTOR_04": 0,
 * "INVERNADERO_02_SECTOR_01": 0,
 * "INVERNADERO_02_SECTOR_02": 0,
 * "INVERNADERO_02_SECTOR_03": 0,
 * "INVERNADERO_02_SECTOR_04": 0,
 * "INVERNADERO_03_SECTOR_01": 0,
 * "INVERNADERO_03_SECTOR_02": 0,
 * "INVERNADERO_03_SECTOR_03": 0,
 * "INVERNADERO_03_SECTOR_04": 0,
 * "INVERNADERO_01_EXTRACTOR": 0,
 * "INVERNADERO_02_EXTRACTOR": 0,
 * "INVERNADERO_03_EXTRACTOR": 0,
 * "RESERVA": 0
 * }
 */

fun String.toRealDataDto( timestamp: Instant = Instant.now(),
                          greenhouseId: String? = null
): RealDataDto {

    // Mapper for extract values
    val jsonNode: JsonNode = objectMapper.readTree(this)


    return RealDataDto(
        timestamp = timestamp,
         TEMPERATURA_INVERNADERO_01 = jsonNode.get("TEMPERATURA INVERNADERO 01").asDouble(),
         HUMEDAD_INVERNADERO_01 = jsonNode.get("HUMEDAD INVERNADERO 01").asDouble(),
         TEMPERATURA_INVERNADERO_02 = jsonNode.get("TEMPERATURA INVERNADERO 02").asDouble(),
         HUMEDAD_INVERNADERO_02 = jsonNode.get("HUMEDAD INVERNADERO 02").asDouble(),
         TEMPERATURA_INVERNADERO_03 = jsonNode.get("TEMPERATURA INVERNADERO 03").asDouble(),
         HUMEDAD_INVERNADERO_03 = jsonNode.get("HUMEDAD INVERNADERO 03").asDouble(),
         INVERNADERO_01_SECTOR_01 = jsonNode.get("INVERNADERO_01_SECTOR_01").asDouble(),
         INVERNADERO_01_SECTOR_02 = jsonNode.get("INVERNADERO_01_SECTOR_02").asDouble(),
         INVERNADERO_01_SECTOR_03 = jsonNode.get("INVERNADERO_01_SECTOR_03").asDouble(),
         INVERNADERO_01_SECTOR_04 = jsonNode.get("INVERNADERO_01_SECTOR_04").asDouble(),
         INVERNADERO_02_SECTOR_01 = jsonNode.get("INVERNADERO_02_SECTOR_01").asDouble(),
         INVERNADERO_02_SECTOR_02 = jsonNode.get("INVERNADERO_02_SECTOR_02").asDouble(),
         INVERNADERO_02_SECTOR_03 = jsonNode.get("INVERNADERO_02_SECTOR_03").asDouble(),
         INVERNADERO_02_SECTOR_04 = jsonNode.get("INVERNADERO_02_SECTOR_04").asDouble(),
         INVERNADERO_03_SECTOR_01 = jsonNode.get("INVERNADERO_03_SECTOR_01").asDouble(),
         INVERNADERO_03_SECTOR_02 = jsonNode.get("INVERNADERO_03_SECTOR_02").asDouble(),
         INVERNADERO_03_SECTOR_03 = jsonNode.get("INVERNADERO_03_SECTOR_03").asDouble(),
         INVERNADERO_03_SECTOR_04 = jsonNode.get("INVERNADERO_03_SECTOR_04").asDouble(),
         INVERNADERO_01_EXTRACTOR = jsonNode.get("INVERNADERO_01_EXTRACTOR").asDouble(),
         INVERNADERO_02_EXTRACTOR = jsonNode.get("INVERNADERO_02_EXTRACTOR").asDouble(),
         INVERNADERO_03_EXTRACTOR = jsonNode.get("INVERNADERO_03_EXTRACTOR").asDouble(),
         RESERVA = jsonNode.get("RESERVA").asDouble(),
        greenhouseId = greenhouseId

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

/**
 * Convierte una lista de RealDataDto a JSON string
 */
fun List<RealDataDto>.toJson(): String {
    return objectMapper.writeValueAsString(this)
}

/**
 * Convierte un RealDataDto a JSON string
 */
fun RealDataDto.toJson(): String {
    return objectMapper.writeValueAsString(this)
}

