package com.apptolast.invernaderos.service

import com.apptolast.invernaderos.entities.dtos.GreenhouseStatisticsDto
import com.apptolast.invernaderos.entities.dtos.GreenhouseSummaryDto
import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.entities.dtos.SensorSummary
import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Servicio de lógica de negocio para datos del invernadero
 *
 * Coordina entre la caché Redis (datos recientes) y TimescaleDB (históricos)
 * siguiendo el patrón de separación de responsabilidades
 */
@Service
class GreenhouseDataService(
    private val cacheService: GreenhouseCacheService,
    private val sensorReadingRepository: SensorReadingRepository
) {

    private val logger = LoggerFactory.getLogger(GreenhouseDataService::class.java)

    /**
     * Obtiene los mensajes más recientes
     * Primero intenta desde Redis (más rápido), si no hay suficientes, complementa desde TimescaleDB
     *
     * @param limit Número de mensajes a obtener
     * @return Lista de mensajes ordenados por timestamp descendente
     */
    fun getRecentMessages(limit: Int = 100): List<RealDataDto> {
        logger.debug("Obteniendo últimos {} mensajes", limit)

        // Obtener desde cache Redis (más rápido)
        val cachedMessages = cacheService.getRecentMessages(limit)

        if (cachedMessages.size >= limit) {
            logger.debug("Obtenidos {} mensajes desde Redis cache", cachedMessages.size)
            return cachedMessages
        }

        // Si no hay suficientes en caché, obtener desde TimescaleDB
        logger.debug("Solo {} mensajes en cache, obteniendo desde TimescaleDB", cachedMessages.size)
        return getMessagesFromTimescaleDB(limit)
    }

    /**
     * Obtiene mensajes en un rango de tiempo específico
     *
     * @param startTime Timestamp de inicio
     * @param endTime Timestamp de fin
     * @return Lista de mensajes en el rango
     */
    fun getMessagesByTimeRange(startTime: Instant, endTime: Instant): List<RealDataDto> {
        logger.debug("Obteniendo mensajes entre {} y {}", startTime, endTime)

        // Primero intentar desde cache Redis
        val cachedMessages = cacheService.getMessagesByTimeRange(startTime, endTime)

        if (cachedMessages.isNotEmpty()) {
            logger.debug("Obtenidos {} mensajes desde Redis cache", cachedMessages.size)
            return cachedMessages
        }

        // Si no hay en cache, obtener desde TimescaleDB
        logger.debug("No hay mensajes en cache, obteniendo desde TimescaleDB")
        return getMessagesFromTimescaleDBByRange(startTime, endTime)
    }

    /**
     * Obtiene el último mensaje recibido
     */
    fun getLatestMessage(): RealDataDto? {
        logger.debug("Obteniendo último mensaje")

        // Primero desde cache
        val cached = cacheService.getLatestMessage()
        if (cached != null) {
            return cached
        }

        // Si no hay en cache, obtener desde TimescaleDB
        val readings = sensorReadingRepository.findTopByOrderByTimeDesc()
        return if (readings.isNotEmpty()) {
            reconstructMessageFromReadings(readings)
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

        val stats = sensorReadingRepository.findStatsBySensorIdAndTimeRange(
            sensorId = sensorId,
            startTime = startTime,
            endTime = endTime
        )

        return if (stats.isNotEmpty()) {
            val stat = stats.first()
            GreenhouseStatisticsDto(
                sensorId = sensorId,
                sensorType = sensorType,
                min = stat[1] as? Double,
                max = stat[2] as? Double,
                avg = stat[3] as? Double,
                count = (stat[4] as? Number)?.toLong() ?: 0,
                lastValue = stat[5] as? Double,
                lastTimestamp = stat[6] as? Instant,
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
        val allSensors = sensorReadingRepository.findDistinctSensorIds()

        val sensors = mutableMapOf<String, SensorSummary>()
        val setpoints = mutableMapOf<String, SensorSummary>()

        allSensors.forEach { sensorId ->
            val stats = sensorReadingRepository.findStatsBySensorIdAndTimeRange(
                sensorId = sensorId,
                startTime = startTime,
                endTime = endTime
            )

            if (stats.isNotEmpty()) {
                val stat = stats.first()
                val summary = SensorSummary(
                    current = stat[5] as? Double,
                    min = stat[1] as? Double,
                    max = stat[2] as? Double,
                    avg = stat[3] as? Double,
                    count = (stat[4] as? Number)?.toLong() ?: 0
                )

                when {
                    sensorId.startsWith("SENSOR_") -> sensors[sensorId] = summary
                    sensorId.startsWith("SETPOINT_") -> setpoints[sensorId] = summary
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
     * Obtiene información del estado de la caché
     */
    fun getCacheInfo(): Map<String, Any> {
        return cacheService.getCacheStats()
    }

    // ========== Métodos privados auxiliares ==========

    private fun getMessagesFromTimescaleDB(limit: Int): List<RealDataDto> {
        val readings = sensorReadingRepository.findTopNOrderByTimeDesc(limit * 5) // Multiplicamos porque hay varios sensores por mensaje

        // Agrupar por timestamp
        val groupedByTime = readings.groupBy { it.time }

        // Reconstruir mensajes
        return groupedByTime.entries
            .sortedByDescending { it.key }
            .take(limit)
            .map { (time, readings) ->
                reconstructMessageFromReadings(readings)
            }
    }

    private fun getMessagesFromTimescaleDBByRange(
        startTime: Instant,
        endTime: Instant
    ): List<RealDataDto> {
        val readings = sensorReadingRepository.findByTimeBetween(startTime, endTime)

        // Agrupar por timestamp
        val groupedByTime = readings.groupBy { it.time }

        // Reconstruir mensajes
        return groupedByTime.entries
            .sortedByDescending { it.key }
            .map { (time, readings) ->
                reconstructMessageFromReadings(readings)
            }
    }

    /**
     * Reconstruye un RealDataDto desde múltiples SensorReading
     */
    private fun reconstructMessageFromReadings(readings: List<SensorReading>): RealDataDto {
        val timestamp = readings.firstOrNull()?.time ?: Instant.now()
        val greenhouseId = readings.firstOrNull()?.greenhouseId

        val sensorMap = readings.associateBy { it.sensorId }

        return RealDataDto(
            timestamp = timestamp,
            temperaturaInvernadero01 = sensorMap["TEMPERATURA_INVERNADERO_01"]?.value,
            humedadInvernadero01 = sensorMap["HUMEDAD_INVERNADERO_01"]?.value,
            temperaturaInvernadero02 = sensorMap["TEMPERATURA_INVERNADERO_02"]?.value,
            humedadInvernadero02 = sensorMap["HUMEDAD_INVERNADERO_02"]?.value,
            temperaturaInvernadero03 = sensorMap["TEMPERATURA_INVERNADERO_03"]?.value,
            humedadInvernadero03 = sensorMap["HUMEDAD_INVERNADERO_03"]?.value,
            invernadero01Sector01 = sensorMap["INVERNADERO_01_SECTOR_01"]?.value,
            invernadero01Sector02 = sensorMap["INVERNADERO_01_SECTOR_02"]?.value,
            invernadero01Sector03 = sensorMap["INVERNADERO_01_SECTOR_03"]?.value,
            invernadero01Sector04 = sensorMap["INVERNADERO_01_SECTOR_04"]?.value,
            invernadero02Sector01 = sensorMap["INVERNADERO_02_SECTOR_01"]?.value,
            invernadero02Sector02 = sensorMap["INVERNADERO_02_SECTOR_02"]?.value,
            invernadero02Sector03 = sensorMap["INVERNADERO_02_SECTOR_03"]?.value,
            invernadero02Sector04 = sensorMap["INVERNADERO_02_SECTOR_04"]?.value,
            invernadero03Sector01 = sensorMap["INVERNADERO_03_SECTOR_01"]?.value,
            invernadero03Sector02 = sensorMap["INVERNADERO_03_SECTOR_02"]?.value,
            invernadero03Sector03 = sensorMap["INVERNADERO_03_SECTOR_03"]?.value,
            invernadero03Sector04 = sensorMap["INVERNADERO_03_SECTOR_04"]?.value,
            invernadero01Extractor = sensorMap["INVERNADERO_01_EXTRACTOR"]?.value,
            invernadero02Extractor = sensorMap["INVERNADERO_02_EXTRACTOR"]?.value,
            invernadero03Extractor = sensorMap["INVERNADERO_03_EXTRACTOR"]?.value,
            reserva = sensorMap["RESERVA"]?.value,
            greenhouseId = greenhouseId
        )
    }

    /**
     * Parsea un string de periodo a rango de tiempo
     */
    private fun parsePeriod(period: String): Pair<Instant, Instant> {
        val endTime = Instant.now()
        val startTime = when {
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

    /**
     * Determina el tipo de sensor según su ID
     */
    private fun determineSensorType(sensorId: String): String {
        return when {
            sensorId.startsWith("SENSOR_") -> "SENSOR"
            sensorId.startsWith("SETPOINT_") -> "SETPOINT"
            else -> "UNKNOWN"
        }
    }
}
