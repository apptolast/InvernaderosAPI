package com.apptolast.invernaderos.mqtt.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Servicio de deduplicacion de lecturas de sensores usando Redis.
 *
 * Evita insertar en sensor_readings (tabla deduplicada) si el mismo code+value
 * ya fue registrado dentro de la ventana temporal configurada.
 *
 * La logica es:
 * - Si no existe clave en Redis para ese code → guardar (primer valor)
 * - Si existe y el value es igual → descartar (duplicado dentro de ventana)
 * - Si existe y el value es diferente → guardar (cambio real)
 * - Si Redis falla → guardar (mejor duplicado que perder dato)
 *
 * Configurable via application.yaml:
 * ```yaml
 * sensor:
 *   deduplication:
 *     enabled: true
 *     window-seconds: 600    # 10 minutos
 *     use-redis: true
 * ```
 */
@Service
class SensorDeduplicationService(
    private val redisTemplate: RedisTemplate<String, Any>,

    @Value("\${sensor.deduplication.enabled:true}")
    private val enabled: Boolean,

    @Value("\${sensor.deduplication.window-seconds:600}")
    private val windowSeconds: Long,

    @Value("\${sensor.deduplication.use-redis:true}")
    private val useRedis: Boolean
) {
    private val logger = LoggerFactory.getLogger(SensorDeduplicationService::class.java)

    private val localCache = ConcurrentHashMap<String, String>()

    private val totalReceived = AtomicLong(0)
    private val totalPassed = AtomicLong(0)
    private val totalDeduped = AtomicLong(0)

    companion object {
        private const val REDIS_KEY_PREFIX = "dedup:"
    }

    init {
        if (enabled) {
            logger.info("Sensor deduplication ENABLED - window: {}s, redis: {}", windowSeconds, useRedis)
        } else {
            logger.info("Sensor deduplication DISABLED - all readings will be persisted to deduped table")
        }
    }

    /**
     * Determina si una lectura debe persistirse en la tabla deduplicada (sensor_readings).
     *
     * @param code Codigo del device/setting
     * @param value Valor actual como string
     * @return true si debe persistirse, false si es duplicado dentro de la ventana
     */
    fun shouldPersistToDeduped(code: String, value: String): Boolean {
        totalReceived.incrementAndGet()

        if (!enabled) {
            totalPassed.incrementAndGet()
            return true
        }

        val shouldPersist = if (useRedis) {
            checkWithRedis(code, value)
        } else {
            checkWithLocalCache(code, value)
        }

        if (shouldPersist) {
            totalPassed.incrementAndGet()
        } else {
            totalDeduped.incrementAndGet()
        }

        return shouldPersist
    }

    private fun checkWithRedis(code: String, value: String): Boolean {
        val redisKey = REDIS_KEY_PREFIX + code

        return try {
            val storedValue = redisTemplate.opsForValue().get(redisKey) as? String

            if (storedValue != null && storedValue == value) {
                // Mismo valor dentro de la ventana → duplicado
                logger.trace("Dedup: code={} value={} - SKIP (duplicate within window)", code, value)
                return false
            }

            // Valor nuevo o diferente → guardar y actualizar Redis
            redisTemplate.opsForValue().set(
                redisKey,
                value,
                Duration.ofSeconds(windowSeconds)
            )
            true

        } catch (e: Exception) {
            logger.warn("Redis error in deduplication for code={}, falling back to persist: {}", code, e.message)
            // Fallback: mejor duplicado que perder dato
            true
        }
    }

    private fun checkWithLocalCache(code: String, value: String): Boolean {
        val storedValue = localCache[code]

        if (storedValue != null && storedValue == value) {
            return false
        }

        localCache[code] = value
        return true
    }

    /**
     * Estadisticas del servicio de deduplicacion
     */
    fun getStats(): DeduplicationStats {
        val received = totalReceived.get()
        val passed = totalPassed.get()
        val deduped = totalDeduped.get()
        val dedupRate = if (received > 0) (deduped.toDouble() / received * 100) else 0.0

        return DeduplicationStats(
            enabled = enabled,
            windowSeconds = windowSeconds,
            useRedis = useRedis,
            totalReceived = received,
            totalPassed = passed,
            totalDeduped = deduped,
            dedupRatePercent = dedupRate
        )
    }
}

data class DeduplicationStats(
    val enabled: Boolean,
    val windowSeconds: Long,
    val useRedis: Boolean,
    val totalReceived: Long,
    val totalPassed: Long,
    val totalDeduped: Long,
    val dedupRatePercent: Double
)
