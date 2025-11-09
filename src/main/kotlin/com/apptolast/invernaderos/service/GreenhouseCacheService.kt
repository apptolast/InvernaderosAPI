package com.apptolast.invernaderos.service

import com.apptolast.invernaderos.entities.dtos.GreenhouseMessageDto
import com.apptolast.invernaderos.entities.dtos.toJson
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Servicio para gestionar la caché de mensajes GREENHOUSE en Redis
 *
 * Utiliza Redis Sorted Set para almacenar los mensajes ordenados por timestamp,
 * permitiendo consultas eficientes por rango de tiempo.
 *
 * Estructura en Redis:
 * - Key: "greenhouse:messages"
 * - Tipo: Sorted Set (ZSET)
 * - Score: timestamp en epoch millis
 * - Value: JSON serializado del GreenhouseMessageDto
 */
@Service
class GreenhouseCacheService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(GreenhouseCacheService::class.java)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val MESSAGES_KEY = "greenhouse:messages"
        private const val MAX_CACHED_MESSAGES = 1000L
        private const val TTL_HOURS = 24L
    }

    /**
     * Guarda un mensaje en la caché de Redis
     *
     * @param message El mensaje a cachear
     */
    fun cacheMessage(message: GreenhouseMessageDto) {
        try {
            val score = message.timestamp.toEpochMilli().toDouble()
            val jsonValue = message.toJson()

            // Agregar el mensaje al sorted set
            redisTemplate.opsForZSet().add(MESSAGES_KEY, jsonValue, score)

            // Mantener solo los últimos MAX_CACHED_MESSAGES mensajes
            val currentSize = redisTemplate.opsForZSet().size(MESSAGES_KEY) ?: 0
            if (currentSize > MAX_CACHED_MESSAGES) {
                // Eliminar los mensajes más antiguos
                val toRemove = currentSize - MAX_CACHED_MESSAGES
                redisTemplate.opsForZSet().removeRange(MESSAGES_KEY, 0, toRemove - 1)
            }

            // Establecer TTL de 24 horas en la key (se renueva con cada write)
            redisTemplate.expire(MESSAGES_KEY, TTL_HOURS, TimeUnit.HOURS)

            logger.debug("Mensaje cacheado en Redis: timestamp=${message.timestamp}")

        } catch (e: Exception) {
            logger.error("Error al cachear mensaje en Redis", e)
        }
    }

    /**
     * Obtiene los últimos N mensajes de la caché
     *
     * @param limit Número de mensajes a obtener (por defecto 100)
     * @return Lista de mensajes ordenados por timestamp descendente (más reciente primero)
     */
    fun getRecentMessages(limit: Int = 100): List<GreenhouseMessageDto> {
        return try {
            // Obtener los últimos 'limit' mensajes del sorted set
            // -limit a -1 significa los últimos 'limit' elementos
            val messages = redisTemplate.opsForZSet()
                .reverseRange(MESSAGES_KEY, 0, limit.toLong() - 1)

            messages?.mapNotNull { json ->
                try {
                    objectMapper.readValue(json, GreenhouseMessageDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error("Error al obtener mensajes recientes desde Redis", e)
            emptyList()
        }
    }

    /**
     * Obtiene mensajes en un rango de tiempo específico
     *
     * @param startTime Timestamp de inicio
     * @param endTime Timestamp de fin
     * @return Lista de mensajes en el rango especificado
     */
    fun getMessagesByTimeRange(startTime: Instant, endTime: Instant): List<GreenhouseMessageDto> {
        return try {
            val minScore = startTime.toEpochMilli().toDouble()
            val maxScore = endTime.toEpochMilli().toDouble()

            val messages = redisTemplate.opsForZSet()
                .reverseRangeByScore(MESSAGES_KEY, minScore, maxScore)

            messages?.mapNotNull { json ->
                try {
                    objectMapper.readValue(json, GreenhouseMessageDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error("Error al obtener mensajes por rango de tiempo desde Redis", e)
            emptyList()
        }
    }

    /**
     * Obtiene el mensaje más reciente de la caché
     *
     * @return El mensaje más reciente o null si no hay mensajes
     */
    fun getLatestMessage(): GreenhouseMessageDto? {
        return try {
            val messages = redisTemplate.opsForZSet()
                .reverseRange(MESSAGES_KEY, 0, 0)

            messages?.firstOrNull()?.let { json ->
                try {
                    objectMapper.readValue(json, GreenhouseMessageDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            }

        } catch (e: Exception) {
            logger.error("Error al obtener el último mensaje desde Redis", e)
            null
        }
    }

    /**
     * Cuenta el número total de mensajes en la caché
     *
     * @return Número de mensajes cacheados
     */
    fun countMessages(): Long {
        return try {
            redisTemplate.opsForZSet().size(MESSAGES_KEY) ?: 0L
        } catch (e: Exception) {
            logger.error("Error al contar mensajes en Redis", e)
            0L
        }
    }

    /**
     * Limpia todos los mensajes de la caché
     */
    fun clearCache() {
        try {
            redisTemplate.delete(MESSAGES_KEY)
            logger.info("Caché de mensajes GREENHOUSE limpiada")
        } catch (e: Exception) {
            logger.error("Error al limpiar la caché de Redis", e)
        }
    }

    /**
     * Obtiene estadísticas de la caché
     */
    fun getCacheStats(): Map<String, Any> {
        return try {
            val count = countMessages()
            val ttl = redisTemplate.getExpire(MESSAGES_KEY, TimeUnit.SECONDS)
            val oldestMessage = redisTemplate.opsForZSet().range(MESSAGES_KEY, 0, 0)?.firstOrNull()
            val latestMessage = redisTemplate.opsForZSet().reverseRange(MESSAGES_KEY, 0, 0)?.firstOrNull()

            mapOf(
                "totalMessages" to count,
                "ttlSeconds" to (ttl ?: -1),
                "hasOldestMessage" to (oldestMessage != null),
                "hasLatestMessage" to (latestMessage != null),
                "maxCapacity" to MAX_CACHED_MESSAGES
            )
        } catch (e: Exception) {
            logger.error("Error al obtener estadísticas de la caché", e)
            emptyMap()
        }
    }
}
