package com.apptolast.invernaderos.features.push

import com.apptolast.invernaderos.features.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Servicio para gestionar tokens FCM. Resuelve `userId` y `tenantId` desde
 * la identidad autenticada (email o username del JWT) — los tokens NUNCA se
 * registran a un usuario que no es el que está enviando la petición.
 *
 * Idempotencia: el endpoint POST hace upsert por `token` UNIQUE. Si el mismo
 * token se registra de nuevo (relogin en el mismo dispositivo, rotación FCM
 * que devuelve el mismo valor, etc.) se actualizan user_id, tenant_id,
 * platform y last_seen_at sin crear filas duplicadas.
 */
@Service
class PushTokenService(
    private val pushTokenRepository: PushTokenRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional("metadataTransactionManager")
    fun register(authenticatedPrincipal: String, token: String, platform: PushPlatform): PushToken {
        val (userId, tenantId) = resolveUserAndTenant(authenticatedPrincipal)

        val existing = pushTokenRepository.findByToken(token)
        return if (existing != null) {
            existing.userId = userId
            existing.tenantId = tenantId
            existing.platform = platform
            existing.lastSeenAt = Instant.now()
            val saved = pushTokenRepository.save(existing)
            logger.info(
                "Push token refreshed: id={} userId={} tenantId={} platform={}",
                saved.id, userId, tenantId, platform
            )
            saved
        } else {
            val saved = pushTokenRepository.save(
                PushToken(
                    userId = userId,
                    tenantId = tenantId,
                    token = token,
                    platform = platform
                )
            )
            logger.info(
                "Push token registered: id={} userId={} tenantId={} platform={}",
                saved.id, userId, tenantId, platform
            )
            saved
        }
    }

    @Transactional("metadataTransactionManager")
    fun unregister(token: String): Boolean {
        val deleted = pushTokenRepository.deleteByToken(token)
        if (deleted > 0) {
            logger.info("Push token unregistered (rows={})", deleted)
        } else {
            logger.debug("Push token unregister: token not found, no-op")
        }
        return deleted > 0
    }

    private fun resolveUserAndTenant(principal: String): Pair<Long, Long> {
        val user = userRepository.findByEmail(principal)
            ?: userRepository.findByUsername(principal)
            ?: throw UsernameNotFoundException(
                "Authenticated user not found in DB: $principal"
            )
        val userId = user.id
            ?: throw IllegalStateException("User has null id (should never happen): ${user.email}")
        return userId to user.tenantId
    }
}
