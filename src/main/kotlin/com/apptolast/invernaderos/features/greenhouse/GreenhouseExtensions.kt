package com.apptolast.invernaderos.features.greenhouse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.jvm.JvmName

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
 * Handles Real Data of the sensors
 * 
 * Note: The JSON keys use inconsistent formats - temperature and humidity fields use spaces
 * (e.g., "TEMPERATURA INVERNADERO 01"), while sector and extractor fields use underscores
 * (e.g., "INVERNADERO_01_SECTOR_01"). This reflects the actual format from the greenhouse system.
 * 
 * Example:
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

fun String.toRealDataDto(
    timestamp: Instant = Instant.now(),
    greenhouseId: String? = null,
    tenantId: String? = null
): RealDataDto {

    // Mapper to extract values
    val jsonNode: JsonNode = objectMapper.readTree(this)


    return RealDataDto(
        timestamp = timestamp,
        temperaturaInvernadero01 = jsonNode.get("TEMPERATURA INVERNADERO 01")?.asDouble(),
        humedadInvernadero01 = jsonNode.get("HUMEDAD INVERNADERO 01")?.asDouble(),
        temperaturaInvernadero02 = jsonNode.get("TEMPERATURA INVERNADERO 02")?.asDouble(),
        humedadInvernadero02 = jsonNode.get("HUMEDAD INVERNADERO 02")?.asDouble(),
        temperaturaInvernadero03 = jsonNode.get("TEMPERATURA INVERNADERO 03")?.asDouble(),
        humedadInvernadero03 = jsonNode.get("HUMEDAD INVERNADERO 03")?.asDouble(),
        invernadero01Sector01 = jsonNode.get("INVERNADERO_01_SECTOR_01")?.asDouble(),
        invernadero01Sector02 = jsonNode.get("INVERNADERO_01_SECTOR_02")?.asDouble(),
        invernadero01Sector03 = jsonNode.get("INVERNADERO_01_SECTOR_03")?.asDouble(),
        invernadero01Sector04 = jsonNode.get("INVERNADERO_01_SECTOR_04")?.asDouble(),
        invernadero02Sector01 = jsonNode.get("INVERNADERO_02_SECTOR_01")?.asDouble(),
        invernadero02Sector02 = jsonNode.get("INVERNADERO_02_SECTOR_02")?.asDouble(),
        invernadero02Sector03 = jsonNode.get("INVERNADERO_02_SECTOR_03")?.asDouble(),
        invernadero02Sector04 = jsonNode.get("INVERNADERO_02_SECTOR_04")?.asDouble(),
        invernadero03Sector01 = jsonNode.get("INVERNADERO_03_SECTOR_01")?.asDouble(),
        invernadero03Sector02 = jsonNode.get("INVERNADERO_03_SECTOR_02")?.asDouble(),
        invernadero03Sector03 = jsonNode.get("INVERNADERO_03_SECTOR_03")?.asDouble(),
        invernadero03Sector04 = jsonNode.get("INVERNADERO_03_SECTOR_04")?.asDouble(),
        invernadero01Extractor = jsonNode.get("INVERNADERO_01_EXTRACTOR")?.asDouble(),
        invernadero02Extractor = jsonNode.get("INVERNADERO_02_EXTRACTOR")?.asDouble(),
        invernadero03Extractor = jsonNode.get("INVERNADERO_03_EXTRACTOR")?.asDouble(),
        reserva = jsonNode.get("RESERVA")?.asDouble(),
        greenhouseId = greenhouseId,
        tenantId = tenantId
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
@JvmName("toJsonGreenhouseMessageDtoList")
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
@JvmName("toJsonRealDataDtoList")
fun List<RealDataDto>.toJson(): String {
    return objectMapper.writeValueAsString(this)
}

/**
 * Convierte un RealDataDto a JSON string
 */
fun RealDataDto.toJson(): String {
    return objectMapper.writeValueAsString(this)
}

