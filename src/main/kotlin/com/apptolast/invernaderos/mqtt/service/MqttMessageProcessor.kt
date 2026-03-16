package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MqttMessageProcessor(
        private val sensorReadingRepository: SensorReadingRepository,
        private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(MqttMessageProcessor::class.java)

    /** Procesa y guarda datos de sensores en TimescaleDB */
    fun processSensorData(greenhouseId: String, sensorType: String, jsonPayload: String) {
        try {
            logger.debug(
                    "Processing sensor data for greenhouse: {}, type: {}",
                    greenhouseId,
                    sensorType
            )

            // Parsear JSON del payload
            val data = objectMapper.readTree(jsonPayload)

            // Validar y convertir greenhouseId a Long
            val greenhouseLongId =
                    try {
                        greenhouseId.toLong()
                    } catch (e: NumberFormatException) {
                        logger.error("Invalid greenhouse ID format: {}", greenhouseId, e)
                        return
                    }

            // Crear lectura de sensor
            val sensorReading =
                    SensorReading(
                            time = data.get("timestamp")?.asText()?.let { Instant.parse(it) }
                                            ?: Instant.now(),
                            code = data.get("sensor_id")?.asText() ?: "unknown",
                            value = data.get("value")?.asText() ?: "0"
                    )

            // Guardar en TimescaleDB
            sensorReadingRepository.save(sensorReading)

            logger.info(
                    "Sensor reading saved - Greenhouse: {}, Code: {}, Value: {}",
                    greenhouseId,
                    sensorReading.code,
                    sensorReading.value
            )

            // Aquí puedes agregar lógica adicional:
            // - Verificar umbrales
            // - Generar alertas
            // - Actualizar cache en Redis
            // - Enviar notificaciones WebSocket

        } catch (e: Exception) {
            logger.error("Error processing sensor data: {}", e.message, e)
            throw e
        }
    }

    /** Procesa estado de actuadores */
    fun processActuatorStatus(greenhouseId: String, jsonPayload: String) {
        try {
            logger.debug("Processing actuator status for greenhouse: {}", greenhouseId)

            val data = objectMapper.readTree(jsonPayload)

            val actuatorId = data.get("actuator_id")?.asText()
            val state = data.get("state")?.asText()
            val value = data.get("value")?.asDouble()

            logger.info(
                    "Actuator status processed - Greenhouse: {}, Actuator: {}, State: {}, Value: {}",
                    greenhouseId,
                    actuatorId,
                    state,
                    value
            )

        } catch (e: Exception) {
            logger.error("Error processing actuator status: {}", e.message, e)
            throw e
        }
    }

}
