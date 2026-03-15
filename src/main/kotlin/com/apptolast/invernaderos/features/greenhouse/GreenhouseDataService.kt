package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.features.statistics.GreenhouseStatisticsDto
import com.apptolast.invernaderos.features.statistics.GreenhouseSummaryDto
import com.apptolast.invernaderos.features.statistics.SensorSummary
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Servicio de lógica de negocio para datos del invernadero
 *
 * Coordina entre la caché Redis (datos recientes) y TimescaleDB (históricos) siguiendo el patrón de
 * separación de responsabilidades
 */
@Service
class GreenhouseDataService(
        private val cacheService: GreenhouseCacheService,
        private val sensorReadingRepository: SensorReadingRepository
) {

    private val logger = LoggerFactory.getLogger(GreenhouseDataService::class.java)

    /**
     * Obtiene los mensajes más recientes para un tenant específico Primero intenta desde Redis (más
     * rápido), si no hay suficientes, complementa desde TimescaleDB
     *
     * @param tenantId ID del tenant (null = DEFAULT para backward compatibility)
     * @param limit Número de mensajes a obtener
     * @return Lista de mensajes ordenados por timestamp descendente
     */
    fun getRecentMessages(tenantId: String? = null, limit: Int = 100): List<RealDataDto> {
        logger.debug("Obteniendo últimos {} mensajes para tenant={}", limit, tenantId)

        // Obtener desde cache Redis (más rápido, aislado por tenant)
        val cachedMessages = cacheService.getRecentMessages(tenantId, limit)

        if (cachedMessages.size >= limit) {
            logger.debug(
                    "Obtenidos {} mensajes desde Redis cache para tenant={}",
                    cachedMessages.size,
                    tenantId
            )
            return cachedMessages
        }

        // Si no hay suficientes en caché, obtener desde TimescaleDB
        logger.debug(
                "Solo {} mensajes en cache, obteniendo desde TimescaleDB para tenant={}",
                cachedMessages.size,
                tenantId
        )
        return getMessagesFromTimescaleDB(tenantId, limit)
    }

    /**
     * Obtiene mensajes en un rango de tiempo específico para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT para backward compatibility)
     * @param startTime Timestamp de inicio
     * @param endTime Timestamp de fin
     * @return Lista de mensajes en el rango
     */
    fun getMessagesByTimeRange(
            tenantId: String? = null,
            startTime: Instant,
            endTime: Instant
    ): List<RealDataDto> {
        logger.debug(
                "Obteniendo mensajes entre {} y {} para tenant={}",
                startTime,
                endTime,
                tenantId
        )

        // Primero intentar desde cache Redis (aislado por tenant)
        val cachedMessages = cacheService.getMessagesByTimeRange(tenantId, startTime, endTime)

        if (cachedMessages.isNotEmpty()) {
            logger.debug(
                    "Obtenidos {} mensajes desde Redis cache para tenant={}",
                    cachedMessages.size,
                    tenantId
            )
            return cachedMessages
        }

        // Si no hay en cache, obtener desde TimescaleDB
        logger.debug(
                "No hay mensajes en cache, obteniendo desde TimescaleDB para tenant={}",
                tenantId
        )
        return getMessagesFromTimescaleDBByRange(tenantId, startTime, endTime)
    }

    /**
     * Obtiene el último mensaje recibido para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT para backward compatibility)
     */
    fun getLatestMessage(tenantId: String? = null): RealDataDto? {
        logger.debug("Obteniendo último mensaje para tenant={}", tenantId)

        // Primero desde cache (aislado por tenant)
        val cached = cacheService.getLatestMessage(tenantId)
        if (cached != null) {
            return cached
        }

        // Si no hay en cache, obtener desde TimescaleDB
        val readings = sensorReadingRepository.findTopNOrderByTimeDesc(10)
        return if (readings.isNotEmpty()) {
            reconstructMessageFromReadings(readings, tenantId)
        } else {
            null
        }
    }

    /**
     * Obtiene estadísticas de un sensor específico en un periodo
     *
     * @param sensorId ID del sensor (ej: "SENSOR_01", "SETPOINT_01")
     * @param period Periodo en formato "1h", "24h", "7d", "30d"
     * @return Estadísticas del sensor
     */
    fun getSensorStatistics(sensorId: String, period: String = "1h"): GreenhouseStatisticsDto? {
        logger.debug("Obteniendo estadísticas de {} para periodo {}", sensorId, period)

        val (startTime, endTime) = parsePeriod(period)
        val sensorType = determineSensorType(sensorId)

        val readings = sensorReadingRepository.findByCodeAndTimeBetween(sensorId, startTime, endTime)

        return if (readings.isNotEmpty()) {
            val values = readings.mapNotNull { it.value.toDoubleOrNull() }
            GreenhouseStatisticsDto(
                    sensorId = sensorId,
                    sensorType = sensorType,
                    min = values.minOrNull(),
                    max = values.maxOrNull(),
                    avg = if (values.isNotEmpty()) values.average() else null,
                    count = readings.size.toLong(),
                    lastValue = values.firstOrNull(),
                    lastTimestamp = readings.first().time,
                    periodStart = startTime,
                    periodEnd = endTime
            )
        } else {
            null
        }
    }

    /**
     * Obtiene un resumen de estadísticas de todos los sensores y setpoints
     *
     * @param period Periodo para calcular estadísticas
     * @return Resumen completo
     */
    fun getSummaryStatistics(period: String = "1h"): GreenhouseSummaryDto {
        logger.debug("Obteniendo resumen de estadísticas para periodo {}", period)

        val (startTime, endTime) = parsePeriod(period)

        // Obtener todos los sensores y setpoints distintos
        val allSensors = sensorReadingRepository.findDistinctCodes()

        val sensors = mutableMapOf<String, SensorSummary>()
        val setpoints = mutableMapOf<String, SensorSummary>()

        allSensors.forEach { code ->
            val readings = sensorReadingRepository.findByCodeAndTimeBetween(code, startTime, endTime)

            if (readings.isNotEmpty()) {
                val values = readings.mapNotNull { it.value.toDoubleOrNull() }
                val summary =
                        SensorSummary(
                                current = values.firstOrNull(),
                                min = values.minOrNull(),
                                max = values.maxOrNull(),
                                avg = if (values.isNotEmpty()) values.average() else null,
                                count = readings.size.toLong()
                        )

                when {
                    code.startsWith("SET-") -> setpoints[code] = summary
                    code.startsWith("DEV-") -> sensors[code] = summary
                }
            }
        }

        val totalMessages = sensorReadingRepository.countByTimeBetween(startTime, endTime)

        return GreenhouseSummaryDto(
                timestamp = Instant.now(),
                totalMessages = totalMessages,
                sensors = sensors,
                setpoints = setpoints,
                periodStart = startTime,
                periodEnd = endTime
        )
    }

    /**
     * Obtiene información del estado de la caché para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT para backward compatibility)
     */
    fun getCacheInfo(tenantId: String? = null): Map<String, Any> {
        return cacheService.getCacheStats(tenantId)
    }

    // ========== Métodos privados auxiliares ==========

    private fun getMessagesFromTimescaleDB(tenantId: String?, limit: Int): List<RealDataDto> {
        val readings =
                sensorReadingRepository.findTopNOrderByTimeDesc(
                        limit * 5
                ) // Multiplicamos porque hay varios sensores por mensaje

        // Agrupar por timestamp
        val groupedByTime = readings.groupBy { it.time }

        // Reconstruir mensajes
        return groupedByTime.entries.sortedByDescending { it.key }.take(limit).map {
                (time, readings) ->
            reconstructMessageFromReadings(readings, tenantId)
        }
    }

    private fun getMessagesFromTimescaleDBByRange(
            tenantId: String?,
            startTime: Instant,
            endTime: Instant
    ): List<RealDataDto> {
        val readings = sensorReadingRepository.findByTimeBetween(startTime, endTime)

        // Agrupar por timestamp
        val groupedByTime = readings.groupBy { it.time }

        // Reconstruir mensajes
        return groupedByTime.entries.sortedByDescending { it.key }.map { (time, readings) ->
            reconstructMessageFromReadings(readings, tenantId)
        }
    }

    /**
     * Reconstruye un RealDataDto desde múltiples SensorReading
     *
     * @param readings Lista de lecturas de sensores para el mismo timestamp
     * @param tenantId ID del tenant (para multi-tenant support)
     */
    private fun reconstructMessageFromReadings(
            readings: List<SensorReading>,
            tenantId: String? = null
    ): RealDataDto {
        val timestamp = readings.firstOrNull()?.time ?: Instant.now()
        val sensorMap = readings.associateBy { it.code }

        return RealDataDto(
                timestamp = timestamp,
                temperaturaInvernadero01 = sensorMap["TEMPERATURA INVERNADERO 01"]?.value?.toDoubleOrNull(),
                humedadInvernadero01 = sensorMap["HUMEDAD INVERNADERO 01"]?.value?.toDoubleOrNull(),
                temperaturaInvernadero02 = sensorMap["TEMPERATURA INVERNADERO 02"]?.value?.toDoubleOrNull(),
                humedadInvernadero02 = sensorMap["HUMEDAD INVERNADERO 02"]?.value?.toDoubleOrNull(),
                temperaturaInvernadero03 = sensorMap["TEMPERATURA INVERNADERO 03"]?.value?.toDoubleOrNull(),
                humedadInvernadero03 = sensorMap["HUMEDAD INVERNADERO 03"]?.value?.toDoubleOrNull(),
                invernadero01Sector01 = sensorMap["INVERNADERO_01_SECTOR_01"]?.value?.toDoubleOrNull(),
                invernadero01Sector02 = sensorMap["INVERNADERO_01_SECTOR_02"]?.value?.toDoubleOrNull(),
                invernadero01Sector03 = sensorMap["INVERNADERO_01_SECTOR_03"]?.value?.toDoubleOrNull(),
                invernadero01Sector04 = sensorMap["INVERNADERO_01_SECTOR_04"]?.value?.toDoubleOrNull(),
                invernadero02Sector01 = sensorMap["INVERNADERO_02_SECTOR_01"]?.value?.toDoubleOrNull(),
                invernadero02Sector02 = sensorMap["INVERNADERO_02_SECTOR_02"]?.value?.toDoubleOrNull(),
                invernadero02Sector03 = sensorMap["INVERNADERO_02_SECTOR_03"]?.value?.toDoubleOrNull(),
                invernadero02Sector04 = sensorMap["INVERNADERO_02_SECTOR_04"]?.value?.toDoubleOrNull(),
                invernadero03Sector01 = sensorMap["INVERNADERO_03_SECTOR_01"]?.value?.toDoubleOrNull(),
                invernadero03Sector02 = sensorMap["INVERNADERO_03_SECTOR_02"]?.value?.toDoubleOrNull(),
                invernadero03Sector03 = sensorMap["INVERNADERO_03_SECTOR_03"]?.value?.toDoubleOrNull(),
                invernadero03Sector04 = sensorMap["INVERNADERO_03_SECTOR_04"]?.value?.toDoubleOrNull(),
                invernadero01Extractor = sensorMap["INVERNADERO_01_EXTRACTOR"]?.value?.toDoubleOrNull(),
                invernadero02Extractor = sensorMap["INVERNADERO_02_EXTRACTOR"]?.value?.toDoubleOrNull(),
                invernadero03Extractor = sensorMap["INVERNADERO_03_EXTRACTOR"]?.value?.toDoubleOrNull(),
                reserva = sensorMap["RESERVA"]?.value?.toDoubleOrNull(),
                greenhouseId = null,
                tenantId = tenantId
        )
    }

    /** Parsea un string de periodo a rango de tiempo */
    private fun parsePeriod(period: String): Pair<Instant, Instant> {
        val endTime = Instant.now()
        val startTime =
                when {
                    period.endsWith("h") -> {
                        val hours = period.removeSuffix("h").toLongOrNull() ?: 1
                        endTime.minus(hours, ChronoUnit.HOURS)
                    }
                    period.endsWith("d") -> {
                        val days = period.removeSuffix("d").toLongOrNull() ?: 1
                        endTime.minus(days, ChronoUnit.DAYS)
                    }
                    period.endsWith("m") -> {
                        val minutes = period.removeSuffix("m").toLongOrNull() ?: 60
                        endTime.minus(minutes, ChronoUnit.MINUTES)
                    }
                    else -> endTime.minus(1, ChronoUnit.HOURS)
                }
        return Pair(startTime, endTime)
    }

    /** Determina el tipo de sensor según su ID */
    private fun determineSensorType(sensorId: String): String {
        return when {
            sensorId.startsWith("SENSOR_") -> "SENSOR"
            sensorId.startsWith("SETPOINT_") -> "SETPOINT"
            else -> "UNKNOWN"
        }
    }
}
