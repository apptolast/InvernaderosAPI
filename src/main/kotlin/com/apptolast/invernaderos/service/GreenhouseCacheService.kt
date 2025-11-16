package com.apptolast.invernaderos.service

import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.entities.dtos.toJson
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Servicio para gestionar la caché de mensajes GREENHOUSE en Redis
 *
 * Utiliza Redis Sorted Set para almacenar los mensajes ordenados por timestamp,
 * permitiendo consultas eficientes por rango de tiempo.
 *
 * MULTI-TENANT ISOLATION:
 * - Cada tenant tiene su propia cache key: "greenhouse:messages:{tenantId}"
 * - Previene acceso cross-tenant a datos sensibles
 * - Requerido después de PostgreSQL multi-tenant migration (V3-V10)
 *
 * Estructura en Redis:
 * - Key: "greenhouse:messages:{tenantId}"
 * - Tipo: Sorted Set (ZSET)
 * - Score: timestamp en epoch millis
 * - Value: JSON serializado del RealDataDto
 */
@Service
class GreenhouseCacheService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(GreenhouseCacheService::class.java)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val MESSAGES_KEY_PREFIX = "greenhouse:messages"
        private const val MAX_CACHED_MESSAGES = 1000L
        private const val TTL_HOURS = 24L
        private const val DEFAULT_TENANT_ID = "DEFAULT"

        /**
         * Genera la cache key específica para un tenant
         * Formato: "greenhouse:messages:{tenantId}"
         *
         * @param tenantId ID del tenant (usa DEFAULT para datos legacy)
         * @return Cache key aislada por tenant
         */
        private fun getMessagesKey(tenantId: String?): String {
            val safeTenantId = tenantId?.takeIf { it.isNotBlank() } ?: DEFAULT_TENANT_ID
            return "$MESSAGES_KEY_PREFIX:$safeTenantId"
        }
    }

    /**
     * Guarda un mensaje en la caché de Redis con aislamiento por tenant
     *
     * @param message El mensaje a cachear (debe incluir tenantId)
     */
    fun cacheMessage(message: RealDataDto) {
        try {
            val tenantId = message.tenantId
            val messagesKey = getMessagesKey(tenantId)
            val score = message.timestamp.toEpochMilli().toDouble()
            val jsonValue = message.toJson()

            // Agregar el mensaje al sorted set del tenant
            redisTemplate.opsForZSet().add(messagesKey, jsonValue, score)

            // Mantener solo los últimos MAX_CACHED_MESSAGES mensajes por tenant
            val currentSize = redisTemplate.opsForZSet().size(messagesKey) ?: 0
            if (currentSize > MAX_CACHED_MESSAGES) {
                // Eliminar los mensajes más antiguos
                val toRemove = currentSize - MAX_CACHED_MESSAGES
                redisTemplate.opsForZSet().removeRange(messagesKey, 0, toRemove - 1)
            }

            // Establecer TTL de 24 horas en la key del tenant (se renueva con cada write)
            redisTemplate.expire(messagesKey, TTL_HOURS, TimeUnit.HOURS)

            logger.debug("Mensaje cacheado en Redis para tenant=$tenantId: timestamp=${message.timestamp}")

        } catch (e: Exception) {
            logger.error("Error al cachear mensaje en Redis para tenant=${message.tenantId}", e)
        }
    }

    /**
     * Obtiene los últimos N mensajes de la caché para un tenant específico
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant para backward compatibility)
     * @param limit Número de mensajes a obtener (por defecto 100)
     * @return Lista de mensajes ordenados por timestamp descendente (más reciente primero)
     */
    fun getRecentMessages(tenantId: String? = null, limit: Int = 100): List<RealDataDto> {
        return try {
            val messagesKey = getMessagesKey(tenantId)

            // Obtener los últimos 'limit' mensajes del sorted set del tenant
            // -limit a -1 significa los últimos 'limit' elementos
            val messages = redisTemplate.opsForZSet()
                .reverseRange(messagesKey, 0, limit.toLong() - 1)

            messages?.mapNotNull { json ->
                try {
                    objectMapper.readValue(json, RealDataDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error("Error al obtener mensajes recientes desde Redis para tenant=$tenantId", e)
            emptyList()
        }
    }

    /**
     * Obtiene mensajes en un rango de tiempo específico para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant para backward compatibility)
     * @param startTime Timestamp de inicio
     * @param endTime Timestamp de fin
     * @return Lista de mensajes en el rango especificado
     */
    fun getMessagesByTimeRange(tenantId: String? = null, startTime: Instant, endTime: Instant): List<RealDataDto> {
        return try {
            val messagesKey = getMessagesKey(tenantId)
            val minScore = startTime.toEpochMilli().toDouble()
            val maxScore = endTime.toEpochMilli().toDouble()

            val messages = redisTemplate.opsForZSet()
                .reverseRangeByScore(messagesKey, minScore, maxScore)

            messages?.mapNotNull { json ->
                try {
                    objectMapper.readValue(json, RealDataDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error("Error al obtener mensajes por rango de tiempo desde Redis para tenant=$tenantId", e)
            emptyList()
        }
    }

    /**
     * Obtiene el mensaje más reciente de la caché para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant para backward compatibility)
     * @return El mensaje más reciente o null si no hay mensajes
     */
    fun getLatestMessage(tenantId: String? = null): RealDataDto? {
        return try {
            val messagesKey = getMessagesKey(tenantId)
            val messages = redisTemplate.opsForZSet()
                .reverseRange(messagesKey, 0, 0)

            messages?.firstOrNull()?.let { json ->
                try {
                    objectMapper.readValue(json, RealDataDto::class.java)
                } catch (e: Exception) {
                    logger.error("Error deserializando mensaje desde Redis: $json", e)
                    null
                }
            }

        } catch (e: Exception) {
            logger.error("Error al obtener el último mensaje desde Redis para tenant=$tenantId", e)
            null
        }
    }

    /**
     * Cuenta el número total de mensajes en la caché para un tenant
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant para backward compatibility)
     * @return Número de mensajes cacheados
     */
    fun countMessages(tenantId: String? = null): Long {
        return try {
            val messagesKey = getMessagesKey(tenantId)
            redisTemplate.opsForZSet().size(messagesKey) ?: 0L
        } catch (e: Exception) {
            logger.error("Error al contar mensajes en Redis para tenant=$tenantId", e)
            0L
        }
    }

    /**
     * Limpia todos los mensajes de la caché para un tenant específico
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant).
     *                 Si se quiere limpiar TODAS las caches de todos los tenants,
     *                 usar clearAllTenantsCache() en su lugar.
     */
    fun clearCache(tenantId: String? = null) {
        try {
            val messagesKey = getMessagesKey(tenantId)
            redisTemplate.delete(messagesKey)
            logger.info("Caché de mensajes GREENHOUSE limpiada para tenant=$tenantId")
        } catch (e: Exception) {
            logger.error("Error al limpiar la caché de Redis para tenant=$tenantId", e)
        }
    }

    /**
     * Limpia las caches de TODOS los tenants (operación administrativa)
     * CUIDADO: Esta operación afecta a todos los tenants del sistema
     */
    fun clearAllTenantsCache() {
        try {
            val pattern = "$MESSAGES_KEY_PREFIX:*"
            val keys = redisTemplate.keys(pattern)
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
                logger.warn("Caché de TODOS los tenants limpiada: ${keys.size} keys eliminadas")
            } else {
                logger.info("No hay caches de tenants para limpiar")
            }
        } catch (e: Exception) {
            logger.error("Error al limpiar las caches de todos los tenants", e)
        }
    }

    /**
     * Obtiene estadísticas de la caché para un tenant específico
     *
     * @param tenantId ID del tenant (null = DEFAULT tenant para backward compatibility)
     */
    fun getCacheStats(tenantId: String? = null): Map<String, Any> {
        return try {
            val messagesKey = getMessagesKey(tenantId)
            val count = countMessages(tenantId)
            val ttl = redisTemplate.getExpire(messagesKey, TimeUnit.SECONDS)
            val oldestMessage = redisTemplate.opsForZSet().range(messagesKey, 0, 0)?.firstOrNull()
            val latestMessage = redisTemplate.opsForZSet().reverseRange(messagesKey, 0, 0)?.firstOrNull()

            mapOf(
                "tenantId" to (tenantId ?: DEFAULT_TENANT_ID),
                "totalMessages" to count,
                "ttlSeconds" to (ttl ?: -1),
                "hasOldestMessage" to (oldestMessage != null),
                "hasLatestMessage" to (latestMessage != null),
                "maxCapacity" to MAX_CACHED_MESSAGES
            )
        } catch (e: Exception) {
            logger.error("Error al obtener estadísticas de la caché para tenant=$tenantId", e)
            emptyMap()
        }
    }
}
