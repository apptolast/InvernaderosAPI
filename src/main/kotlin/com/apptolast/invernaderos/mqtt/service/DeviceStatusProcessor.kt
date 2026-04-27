package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertMqttInboundAdapter
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReadingRaw
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCurrentValueRepository
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRawRepository
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Procesador de mensajes de estado de dispositivos/settings.
 *
 * Escribe en 3 tablas de TimescaleDB con diferentes propositos:
 * 1. device_current_values  - UPSERT siempre (ultimo valor para WebSocket)
 * 2. sensor_readings_raw    - INSERT siempre (archivo fiel sin dedup)
 * 3. sensor_readings        - INSERT solo si pasa dedup (serie temporal limpia)
 *
 * Las escrituras se acumulan en buffers y se persisten en batch cada segundo.
 */
@Service
class DeviceStatusProcessor(
    private val sensorReadingRepository: SensorReadingRepository,
    private val sensorReadingRawRepository: SensorReadingRawRepository,
    private val deviceCurrentValueRepository: DeviceCurrentValueRepository,
    private val deduplicationService: SensorDeduplicationService,
    private val alertMqttInboundAdapter: AlertMqttInboundAdapter
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Cache en memoria con el ultimo valor conocido de cada codigo.
     * Se mantiene para logging/debug. La deduplicacion real la hace Redis.
     */
    val lastKnownValues = ConcurrentHashMap<String, String>()

    /**
     * Buffer de lecturas para la tabla deduplicada (sensor_readings).
     * Solo se encolan las que pasan el dedup check.
     */
    private val dedupedReadings = ConcurrentLinkedQueue<SensorReading>()

    /**
     * Buffer de lecturas para la tabla raw (sensor_readings_raw).
     * Se encolan TODAS las lecturas sin filtrar.
     */
    private val rawReadings = ConcurrentLinkedQueue<SensorReadingRaw>()

    /**
     * Buffer de actualizaciones para device_current_values.
     * Solo se guarda el ultimo valor por code (sobreescribe anteriores).
     */
    private val currentValueUpdates = ConcurrentHashMap<String, Pair<String, Instant>>()

    /**
     * Procesa una actualizacion de estado individual.
     *
     * 1. Siempre encola para raw (archivo fiel)
     * 2. Siempre actualiza current_values (ultimo valor para WebSocket)
     * 3. Solo encola para deduped si pasa el dedup check (Redis)
     */
    fun processStatusUpdate(code: String, value: String) {
        val now = Instant.now()
        lastKnownValues[code] = value

        // 1. Siempre: encolar para sensor_readings_raw
        rawReadings.add(SensorReadingRaw(time = now, code = code, value = value))

        // 2. Siempre: actualizar device_current_values (solo ultimo por code)
        currentValueUpdates[code] = Pair(value, now)

        // 3. Dedup check: solo encolar para sensor_readings si pasa
        if (deduplicationService.shouldPersistToDeduped(code, value)) {
            dedupedReadings.add(SensorReading(time = now, code = code, value = value))
            logger.debug("Dedup PASS - code: {}, value: {}", code, value)
        }

        // 4. Alert routing: ALT- codes additionally trigger the alert state machine.
        // handleSignal catches all exceptions internally (see AlertMqttInboundAdapter)
        // so a downstream alert misconfiguration cannot break the telemetry path above.
        if (code.startsWith("ALT-")) {
            alertMqttInboundAdapter.handleSignal(code, value)
        }
    }

    /**
     * Flush periodico: persiste todos los cambios acumulados en batch.
     * Se ejecuta cada segundo para agrupar los ~78 mensajes individuales
     * en operaciones batch por tabla.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional("timescaleTransactionManager")
    fun flushPendingChanges() {
        // 1. Flush device_current_values (UPSERT batch)
        flushCurrentValues()

        // 2. Flush sensor_readings_raw (INSERT batch)
        flushRawReadings()

        // 3. Flush sensor_readings deduped (INSERT batch)
        flushDedupedReadings()
    }

    private fun flushCurrentValues() {
        if (currentValueUpdates.isEmpty()) return

        val updates = HashMap(currentValueUpdates)
        currentValueUpdates.clear()

        updates.forEach { (code, pair) ->
            val (value, timestamp) = pair
            deviceCurrentValueRepository.upsert(code, value, timestamp)
        }

        logger.debug("Upserted {} current values", updates.size)
    }

    private fun flushRawReadings() {
        val rawToPersist = drainQueue(rawReadings)
        if (rawToPersist.isEmpty()) return

        sensorReadingRawRepository.saveAll(rawToPersist)
        logger.debug("Persisted {} raw readings", rawToPersist.size)
    }

    private fun flushDedupedReadings() {
        val dedupedToPersist = drainQueue(dedupedReadings)
        if (dedupedToPersist.isEmpty()) return

        sensorReadingRepository.saveAll(dedupedToPersist)
        logger.info("Persisted {} deduped readings (raw total in this batch included above)", dedupedToPersist.size)
    }

    private fun <T> drainQueue(queue: ConcurrentLinkedQueue<T>): List<T> {
        val result = mutableListOf<T>()
        var entry = queue.poll()
        while (entry != null) {
            result.add(entry)
            entry = queue.poll()
        }
        return result
    }
}
