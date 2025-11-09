package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MqttMessageProcessor(
    private val sensorReadingRepository: SensorReadingRepository,
    private val objectMapper: ObjectMapper
) {

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
    fun processGreenhouseData(jsonPayload: String) {
        try {
            println("🔄 Procesando datos del greenhouse...")

            // Parsear JSON
            val data = objectMapper.readTree(jsonPayload)
            val timestamp = Instant.now()

            // Procesar cada campo
            data.fields().forEach { (key, value) ->
                val sensorValue = value.asDouble()

                // Determinar el tipo de sensor
                val sensorType = when {
                    key.startsWith("SENSOR_") -> "SENSOR"
                    key.startsWith("SETPOINT_") -> "SETPOINT"
                    else -> "UNKNOWN"
                }

                // Solo guardar si el valor no es 0 (o guardarlo siempre, según tu lógica)
                if (sensorValue != 0.0 || sensorType == "SENSOR") {
                    val sensorReading = SensorReading(
                        time = timestamp,
                        sensorId = key,
                        greenhouseId = "001", // ID fijo o puedes pasarlo como parámetro
                        sensorType = sensorType,
                        value = sensorValue,
                        unit = determineUnit(key)
                    )

                    // Guardar en TimescaleDB
                    sensorReadingRepository.save(sensorReading)

                    println("✅ Lectura guardada:")
                    println("   Sensor: $key")
                    println("   Tipo: $sensorType")
                    println("   Valor: $sensorValue")
                }
            }

            println("✅ Procesamiento completado")

        } catch (e: Exception) {
            println("❌ Error procesando datos: ${e.message}")
            e.printStackTrace()
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