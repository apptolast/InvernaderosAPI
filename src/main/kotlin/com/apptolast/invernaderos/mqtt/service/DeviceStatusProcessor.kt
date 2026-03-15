package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Evento publicado cuando hay cambios de estado en dispositivos/settings.
 * Usado por el WebSocket handler para broadcast a clientes.
 */
data class DeviceStatusUpdateEvent(
    val source: Any,
    val changes: List<DeviceStatusChange>
)

data class DeviceStatusChange(
    val code: String,
    val value: String,
    val timestamp: Instant
)

/**
 * Procesador de mensajes de estado de dispositivos/settings.
 *
 * Implementa dos optimizaciones clave:
 * 1. Change Detection: Solo guarda en DB cuando el valor cambia
 * 2. Batch Insert: Acumula cambios y los guarda en batch cada segundo
 *
 * Además, un snapshot scheduler guarda todos los valores cada 5 minutos
 * para garantizar puntos de referencia en gráficas temporales.
 */
@Service
class DeviceStatusProcessor(
    private val sensorReadingRepository: SensorReadingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Cache en memoria con el último valor conocido de cada código.
     * Se usa para change detection: solo guardamos cuando el valor cambia.
     */
    val lastKnownValues = ConcurrentHashMap<String, String>()

    /**
     * Buffer de cambios pendientes de persistir.
     * Thread-safe para soportar múltiples mensajes MQTT concurrentes.
     */
    private val pendingChanges = ConcurrentLinkedQueue<SensorReading>()

    /**
     * Procesa una actualización de estado individual.
     * Compara con el último valor conocido y solo encola para persistencia si cambió.
     */
    fun processStatusUpdate(code: String, value: String) {
        val previousValue = lastKnownValues.put(code, value)

        // Solo guardar si el valor cambió (o es la primera vez que vemos este código)
        if (previousValue == null || previousValue != value) {
            val reading = SensorReading(
                time = Instant.now(),
                code = code,
                value = value
            )
            pendingChanges.add(reading)

            logger.debug("Status change detected - code: {}, previous: {}, new: {}", code, previousValue, value)
        }
    }

    /**
     * Flush periódico: persiste todos los cambios acumulados en batch.
     * Se ejecuta cada segundo para agrupar los ~78 mensajes individuales
     * en una sola operación de base de datos.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional("timescaleTransactionManager")
    fun flushPendingChanges() {
        val changesToPersist = mutableListOf<SensorReading>()

        // Drenar la cola de cambios pendientes
        var entry = pendingChanges.poll()
        while (entry != null) {
            changesToPersist.add(entry)
            entry = pendingChanges.poll()
        }

        if (changesToPersist.isEmpty()) {
            return
        }

        // Batch INSERT
        sensorReadingRepository.saveAll(changesToPersist)

        logger.info("Persisted {} status changes in batch", changesToPersist.size)

        // Publicar evento para WebSocket broadcast
        val changes = changesToPersist.map { DeviceStatusChange(it.code, it.value, it.time) }
        eventPublisher.publishEvent(DeviceStatusUpdateEvent(source = this, changes = changes))
    }

    /**
     * Snapshot periódico: guarda TODOS los valores conocidos cada 5 minutos.
     * Esto garantiza puntos de referencia para gráficas temporales,
     * incluso cuando los valores no han cambiado.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional("timescaleTransactionManager")
    fun saveSnapshot() {
        val currentValues = HashMap(lastKnownValues)

        if (currentValues.isEmpty()) {
            logger.debug("Snapshot skipped - no known values yet")
            return
        }

        val snapshotTime = Instant.now()
        val snapshotEntries = currentValues.map { (code, value) ->
            SensorReading(time = snapshotTime, code = code, value = value)
        }

        sensorReadingRepository.saveAll(snapshotEntries)

        logger.info("Snapshot saved: {} entries at {}", snapshotEntries.size, snapshotTime)
    }
}
