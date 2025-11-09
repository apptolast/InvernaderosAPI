package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.entities.dtos.GreenhouseMessageDto
import com.apptolast.invernaderos.entities.dtos.toGreenhouseMessageDto
import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import com.apptolast.invernaderos.service.GreenhouseCacheService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MqttMessageProcessor(
    private val sensorReadingRepository: SensorReadingRepository,
    private val objectMapper: ObjectMapper,
    private val greenhouseCacheService: GreenhouseCacheService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(MqttMessageProcessor::class.java)

    /**
     * Procesa y guarda datos de sensores en TimescaleDB
     */
    fun processSensorData(greenhouseId: String, sensorType: String, jsonPayload: String) {
        try {
            println("🔄 Procesando datos de sensor...")

            // Parsear JSON del payload
            val data = objectMapper.readTree(jsonPayload)

            // Crear lectura de sensor
            val sensorReading = SensorReading(
                time = data.get("timestamp")?.asText()?.let { Instant.parse(it) } ?: Instant.now(),
                sensorId = data.get("sensor_id")?.asText() ?: "unknown",
                greenhouseId = greenhouseId,
                sensorType = sensorType,
                value = data.get("value")?.asDouble() ?: 0.0,
                unit = data.get("unit")?.asText()
            )

            // Guardar en TimescaleDB
            sensorReadingRepository.save(sensorReading)

            println("✅ Lectura guardada en TimescaleDB:")
            println("   Greenhouse: $greenhouseId")
            println("   Sensor: ${sensorReading.sensorId}")
            println("   Tipo: $sensorType")
            println("   Valor: ${sensorReading.value} ${sensorReading.unit}")

            // Aquí puedes agregar lógica adicional:
            // - Verificar umbrales
            // - Generar alertas
            // - Actualizar cache en Redis
            // - Enviar notificaciones WebSocket

        } catch (e: Exception) {
            println("❌ Error procesando datos de sensor: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Procesa estado de actuadores
     */
    fun processActuatorStatus(greenhouseId: String, jsonPayload: String) {
        try {
            println("🔄 Procesando estado de actuador...")

            val data = objectMapper.readTree(jsonPayload)

            val actuatorId = data.get("actuator_id")?.asText()
            val state = data.get("state")?.asText()
            val value = data.get("value")?.asDouble()

            println("✅ Estado de actuador procesado:")
            println("   Greenhouse: $greenhouseId")
            println("   Actuador: $actuatorId")
            println("   Estado: $state")
            println("   Valor: $value")

            // Aquí puedes:
            // - Actualizar estado en PostgreSQL (tabla actuators)
            // - Registrar cambios de estado
            // - Notificar a usuarios

        } catch (e: Exception) {
            println("❌ Error procesando estado de actuador: ${e.message}")
            e.printStackTrace()
        }
    }


    /**
     * Procesa el payload del topic GREENHOUSE
     * Formato: {"SENSOR_01":1.23,"SENSOR_02":0,"SETPOINT_01":5.67,"SETPOINT_02":0}
     */
    fun processGreenhouseData(jsonPayload: String, greenhouseId: String = "001") {
        try {
            logger.debug("Procesando datos del greenhouse: $greenhouseId")

            val timestamp = Instant.now()

            // 1. Convertir a DTO usando extension function
            val messageDto = jsonPayload.toGreenhouseMessageDto(
                timestamp = timestamp,
                greenhouseId = greenhouseId
            )

            // 2. Cachear el mensaje completo en Redis
            greenhouseCacheService.cacheMessage(messageDto)
            logger.debug("Mensaje cacheado en Redis")

            // 3. Parsear JSON para guardar en TimescaleDB
            val data = objectMapper.readTree(jsonPayload)

            // 4. Procesar cada campo y guardar en TimescaleDB
            data.fields().forEach { (key, value) ->
                val sensorValue = value.asDouble()

                // Determinar el tipo de sensor
                val sensorType = when {
                    key.startsWith("SENSOR_") -> "SENSOR"
                    key.startsWith("SETPOINT_") -> "SETPOINT"
                    else -> "UNKNOWN"
                }

                // Guardar todos los valores en TimescaleDB
                val sensorReading = SensorReading(
                    time = timestamp,
                    sensorId = key,
                    greenhouseId = greenhouseId,
                    sensorType = sensorType,
                    value = sensorValue,
                    unit = determineUnit(key)
                )

                // Guardar en TimescaleDB
                sensorReadingRepository.save(sensorReading)

                logger.trace("Lectura guardada: $key = $sensorValue")
            }

            // 5. Publicar evento para WebSocket (para transmisión en tiempo real)
            eventPublisher.publishEvent(GreenhouseMessageEvent(this, messageDto))
            logger.debug("Evento publicado para WebSocket")

            logger.info("Procesamiento completado: {} sensores/setpoints guardados", data.size())

        } catch (e: Exception) {
            logger.error("Error procesando datos del greenhouse: ${e.message}", e)
            throw e
        }
    }

    /**
     * Determina la unidad de medida según el nombre del sensor
     */
    private fun determineUnit(sensorKey: String): String {
        return when {
            sensorKey.contains("TEMP") -> "°C"
            sensorKey.contains("HUMIDITY") -> "%"
            sensorKey.contains("PRESSURE") -> "hPa"
            sensorKey.contains("SETPOINT") -> "value"
            else -> "unit"
        }
    }
}

/**
 * Evento de Spring que se publica cuando llega un nuevo mensaje GREENHOUSE
 * Permite la comunicación desacoplada entre componentes (por ejemplo, para WebSocket)
 */
data class GreenhouseMessageEvent(
    val source: Any,
    val message: GreenhouseMessageDto
) : org.springframework.context.ApplicationEvent(source)