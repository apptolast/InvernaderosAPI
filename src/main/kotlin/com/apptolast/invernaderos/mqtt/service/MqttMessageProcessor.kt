package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.entities.dtos.toRealDataDto
import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.metadata.GreenhouseRepository
import com.apptolast.invernaderos.repositories.metadata.TenantRepository
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import com.apptolast.invernaderos.service.GreenhouseCacheService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class MqttMessageProcessor(
    private val sensorReadingRepository: SensorReadingRepository,
    private val tenantRepository: TenantRepository,
    private val greenhouseRepository: GreenhouseRepository,
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
            logger.debug("Processing sensor data for greenhouse: {}, type: {}", greenhouseId, sensorType)

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

            logger.info("Sensor reading saved - Greenhouse: {}, Sensor: {}, Type: {}, Value: {} {}",
                greenhouseId, sensorReading.sensorId, sensorType, sensorReading.value, sensorReading.unit)

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

    /**
     * Procesa estado de actuadores
     */
    fun processActuatorStatus(greenhouseId: String, jsonPayload: String) {
        try {
            logger.debug("Processing actuator status for greenhouse: {}", greenhouseId)

            val data = objectMapper.readTree(jsonPayload)

            val actuatorId = data.get("actuator_id")?.asText()
            val state = data.get("state")?.asText()
            val value = data.get("value")?.asDouble()

            logger.info("Actuator status processed - Greenhouse: {}, Actuator: {}, State: {}, Value: {}",
                greenhouseId, actuatorId, state, value)

            // Aquí puedes:
            // - Actualizar estado en PostgreSQL (tabla actuators)
            // - Registrar cambios de estado
            // - Notificar a usuarios

        } catch (e: Exception) {
            logger.error("Error processing actuator status: {}", e.message, e)
            throw e
        }
    }


    /**
     * Procesa el payload del topic GREENHOUSE multi-tenant
     *
     * Soporta dos formatos:
     * 1. Legacy: {"SENSOR_01":1.23,"SENSOR_02":0,"SETPOINT_01":5.67}
     * 2. Nuevo: {"TEMPERATURA INVERNADERO 01":25.3,"HUMEDAD INVERNADERO 01":60.2,...}
     * 3. Híbrido: {"empresaID_sensorID_valor": 25.3, ...}
     *
     * Usa @Transactional para garantizar consistencia y optimizar con batch inserts
     *
     * @param jsonPayload Payload JSON del mensaje MQTT
     * @param tenantId ID del tenant extraído del topic (e.g., "SARA", "001", "DEFAULT")
     * @throws IllegalArgumentException si el tenant no existe
     * @throws IllegalStateException si no se encuentra greenhouse activo para el tenant
     */
    @Transactional
    fun processGreenhouseData(jsonPayload: String, tenantId: String) {
        try {
            logger.debug("Procesando datos del tenant: $tenantId")

            // 1. VALIDAR TENANT - lookup por mqttTopicPrefix
            val tenant = tenantRepository.findByMqttTopicPrefix(tenantId)
                ?: throw IllegalArgumentException("Tenant no encontrado con mqttTopicPrefix: $tenantId")

            logger.debug("Tenant validado: {} (UUID: {})", tenant.name, tenant.id)

            // 2. BUSCAR GREENHOUSE ACTIVO del tenant (usar el primero activo)
            val greenhouse = greenhouseRepository.findByTenantIdAndIsActive(tenant.id!!, true)
                .firstOrNull()
                ?: throw IllegalStateException("No se encontró greenhouse activo para tenant: ${tenant.name} (mqttTopicPrefix: $tenantId)")

            logger.debug("Greenhouse encontrado: {} (UUID: {})", greenhouse.name, greenhouse.id)

            val timestamp = Instant.now()

            // 3. Convertir a DTO usando extension function (mantener compatibilidad WebSocket)
            val messageDto = jsonPayload.toRealDataDto(
                timestamp = timestamp,
                greenhouseId = tenantId  // Para WebSocket/cache, usar tenantId como string
            )

            // 4. Cachear el mensaje completo en Redis
            greenhouseCacheService.cacheMessage(messageDto)
            logger.debug("Mensaje cacheado en Redis")

            // 5. Parsear JSON para guardar en TimescaleDB
            val data = objectMapper.readTree(jsonPayload)

            // 6. Procesar cada campo y crear lista de lecturas con UUIDs
            val sensorReadings = data.fields().asSequence().map { (key, value) ->
                val sensorValue = value.asDouble()

                // Determinar el tipo de sensor (mejorado para formato híbrido)
                val sensorType = when {
                    key.startsWith("SENSOR_") -> "SENSOR"
                    key.startsWith("SETPOINT_") -> "SETPOINT"
                    key.contains("TEMPERATURA") || key.contains("TEMP") -> "TEMPERATURE"
                    key.contains("HUMEDAD") || key.contains("HUM") -> "HUMIDITY"
                    key.contains("INVERNADERO") && key.contains("SECTOR") -> "SECTOR"
                    key.contains("EXTRACTOR") -> "EXTRACTOR"
                    else -> "UNKNOWN"
                }

                // Crear lectura de sensor con UUIDs (CRÍTICO: greenhouseId y tenantId como UUID)
                SensorReading(
                    time = timestamp,
                    sensorId = key,
                    greenhouseId = greenhouse.id!!,  // UUID del greenhouse
                    tenantId = tenant.id,             // UUID del tenant (denormalizado)
                    sensorType = sensorType,
                    value = sensorValue,
                    unit = determineUnit(key)
                ).also {
                    logger.trace("Lectura creada: $key = $sensorValue (greenhouse UUID: ${greenhouse.id}, tenant UUID: ${tenant.id})")
                }
            }.toList()

            // 7. Guardar todas las lecturas en una sola operación batch (más eficiente)
            sensorReadingRepository.saveAll(sensorReadings)
            logger.debug("Guardadas {} lecturas en TimescaleDB (batch operation)", sensorReadings.size)

            // 8. Publicar evento para WebSocket (para transmisión en tiempo real)
            eventPublisher.publishEvent(GreenhouseMessageEvent(this, messageDto))
            logger.debug("Evento publicado para WebSocket")

            logger.info("✅ Procesamiento completado - Tenant: {} ({}), Greenhouse: {} ({}), {} lecturas guardadas",
                tenant.name, tenantId, greenhouse.name, greenhouse.id, sensorReadings.size)

        } catch (e: IllegalArgumentException) {
            logger.error("❌ Error de validación: ${e.message}")
            throw e
        } catch (e: IllegalStateException) {
            logger.error("❌ Error de estado: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("❌ Error procesando datos del tenant $tenantId: ${e.message}", e)
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
class GreenhouseMessageEvent(
    source: Any,
    val message: RealDataDto
) : org.springframework.context.ApplicationEvent(source)