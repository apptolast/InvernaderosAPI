package com.apptolast.invernaderos.mqtt.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Rate limiter para lecturas de sensores.
 *
 * Evita guardar lecturas duplicadas o muy cercanas en tiempo para reducir
 * el volumen de datos en TimescaleDB.
 *
 * Configurable vía application.yaml:
 * ```yaml
 * sensor:
 *   rate-limiter:
 *     enabled: true
 *     min-interval-seconds: 30  # Mínimo 30 segundos entre lecturas del mismo sensor
 *     use-redis: true           # Usar Redis (recomendado para multi-pod)
 * ```
 *
 * IMPORTANTE: Los datos siguen llegando a Redis cache y WebSocket en tiempo real,
 * solo se reduce lo que se guarda en TimescaleDB.
 */
@Service
class SensorRateLimiter(
    private val redisTemplate: RedisTemplate<String, Any>,

    @Value("\${sensor.rate-limiter.enabled:false}")
    private val enabled: Boolean,

    @Value("\${sensor.rate-limiter.min-interval-seconds:30}")
    private val minIntervalSeconds: Long,

    @Value("\${sensor.rate-limiter.use-redis:true}")
    private val useRedis: Boolean
) {
    private val logger = LoggerFactory.getLogger(SensorRateLimiter::class.java)

    // Cache local para modo sin Redis o fallback
    private val localCache = ConcurrentHashMap<String, Instant>()

    // Contadores para estadísticas
    private val totalReceived = AtomicLong(0)
    private val totalSaved = AtomicLong(0)
    private val totalDropped = AtomicLong(0)

    companion object {
        private const val REDIS_KEY_PREFIX = "sensor:rate-limit:"
    }

    init {
        if (enabled) {
            logger.warn("╔════════════════════════════════════════════════════════════╗")
            logger.warn("║  SENSOR RATE LIMITER ACTIVADO                              ║")
            logger.warn("║                                                            ║")
            logger.warn("║  Intervalo mínimo entre lecturas: {} segundos           ║", 
                String.format("%3d", minIntervalSeconds))
            logger.warn("║  Usando Redis: {}                                        ║", 
                if (useRedis) "SÍ " else "NO ")
            logger.warn("║                                                            ║")
            logger.warn("║  Los datos siguen llegando a Redis cache y WebSocket,     ║")
            logger.warn("║  solo se reduce lo guardado en TimescaleDB.               ║")
            logger.warn("╚════════════════════════════════════════════════════════════╝")
        } else {
            logger.info("Sensor Rate Limiter DESACTIVADO - Todas las lecturas se guardarán")
        }
    }

    /**
     * Verifica si una lectura debe ser guardada en TimescaleDB.
     *
     * @param sensorId ID del sensor
     * @param greenhouseId ID del greenhouse (UUID como String)
     * @return true si la lectura debe guardarse, false si debe descartarse
     */
    fun shouldSave(sensorId: String, greenhouseId: String): Boolean {
        totalReceived.incrementAndGet()

        if (!enabled) {
            totalSaved.incrementAndGet()
            return true
        }

        val key = "$greenhouseId:$sensorId"
        val now = Instant.now()

        val shouldSave = if (useRedis) {
            shouldSaveWithRedis(key, now)
        } else {
            shouldSaveWithLocalCache(key, now)
        }

        if (shouldSave) {
            totalSaved.incrementAndGet()
        } else {
            totalDropped.incrementAndGet()
        }

        return shouldSave
    }

    private fun shouldSaveWithRedis(key: String, now: Instant): Boolean {
        val redisKey = REDIS_KEY_PREFIX + key

        return try {
            val lastReadingStr = redisTemplate.opsForValue().get(redisKey) as? String

            if (lastReadingStr != null) {
                val lastTime = Instant.parse(lastReadingStr)
                val elapsed = Duration.between(lastTime, now).seconds

                if (elapsed < minIntervalSeconds) {
                    logger.trace(
                        "Rate limited: sensor={}, elapsed={}s < min={}s",
                        key, elapsed, minIntervalSeconds
                    )
                    return false
                }
            }

            // Guardar timestamp actual con TTL
            redisTemplate.opsForValue().set(
                redisKey,
                now.toString(),
                Duration.ofSeconds(minIntervalSeconds * 2)
            )
            true

        } catch (e: Exception) {
            logger.warn("Redis error in rate limiter, using local cache: {}", e.message)
            // Fallback a cache local en caso de error de Redis
            shouldSaveWithLocalCache(key, now)
        }
    }

    private fun shouldSaveWithLocalCache(key: String, now: Instant): Boolean {
        val lastTime = localCache[key]

        if (lastTime != null) {
            val elapsed = Duration.between(lastTime, now).seconds

            if (elapsed < minIntervalSeconds) {
                return false
            }
        }

        localCache[key] = now

        // Limpieza periódica del cache local (cada 1000 entradas)
        if (localCache.size > 1000) {
            cleanupLocalCache(now)
        }

        return true
    }

    private fun cleanupLocalCache(now: Instant) {
        val threshold = now.minusSeconds(minIntervalSeconds * 2)
        localCache.entries.removeIf { it.value.isBefore(threshold) }
        logger.debug("Local cache cleanup: {} entries remaining", localCache.size)
    }

    /**
     * Estadísticas del rate limiter
     */
    fun getStats(): RateLimiterStats {
        val received = totalReceived.get()
        val saved = totalSaved.get()
        val dropped = totalDropped.get()
        val dropRate = if (received > 0) (dropped.toDouble() / received * 100) else 0.0

        return RateLimiterStats(
            enabled = enabled,
            minIntervalSeconds = minIntervalSeconds,
            useRedis = useRedis,
            totalReceived = received,
            totalSaved = saved,
            totalDropped = dropped,
            dropRatePercent = dropRate,
            localCacheSize = localCache.size
        )
    }

    /**
     * Resetea las estadísticas (útil para tests)
     */
    fun resetStats() {
        totalReceived.set(0)
        totalSaved.set(0)
        totalDropped.set(0)
    }
}

/**
 * Data class para estadísticas del rate limiter
 */
data class RateLimiterStats(
    val enabled: Boolean,
    val minIntervalSeconds: Long,
    val useRedis: Boolean,
    val totalReceived: Long,
    val totalSaved: Long,
    val totalDropped: Long,
    val dropRatePercent: Double,
    val localCacheSize: Int
)
