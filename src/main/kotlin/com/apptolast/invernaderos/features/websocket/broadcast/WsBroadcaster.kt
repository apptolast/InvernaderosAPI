package com.apptolast.invernaderos.features.websocket.broadcast

import com.apptolast.invernaderos.features.user.UserRepository
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusAssembler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Routes a fresh per-tenant snapshot to every active user of the tenant
 * across the cluster. Two entry points:
 *
 *  - [broadcastTenantStatus]: invoked by `@TransactionalEventListener
 *    (AFTER_COMMIT)`. Delivers locally **and** republishes via Redis pub/sub
 *    so other pods deliver to their own connected sessions. Async on the
 *    [wsBroadcastExecutor] so a slow `convertAndSendToUser` never blocks
 *    the listener thread.
 *  - [deliverLocallyOnly]: invoked by `WsRedisListener` after receiving
 *    a Redis pub/sub message. Skips the Redis hop on purpose to avoid an
 *    infinite loop. Also async (every pod handles its own users in parallel).
 *
 * **No-op fast path**: when `findByTenantIdAndIsActiveTrue` ∩
 * `simpUserRegistry.users` is empty (no active user of this tenant has a
 * STOMP session on this pod) the method returns without re-assembling the
 * snapshot — Redis fan-out will reach the right pod. This keeps the cost
 * negligible on pods that don't host the targeted users.
 *
 * **Error isolation**: every step is wrapped in try/catch. A serialisation
 * failure or a closed STOMP session on one user must not stop delivery to
 * the others. Errors are logged and swallowed (the next flush carries the
 * fresh state anyway). Mirrors the pattern in [com.apptolast.invernaderos
 * .features.push.infrastructure.adapter.output.AlertActivationPushListener].
 */
@Component
class WsBroadcaster(
    private val assembler: GreenhouseStatusAssembler,
    private val userRepository: UserRepository,
    private val simpUserRegistry: SimpUserRegistry,
    private val messagingTemplate: SimpMessagingTemplate,
    @Qualifier("redisTemplate") private val redisTemplate: RedisTemplate<String, Any>,
    @Qualifier("wsBroadcastChannel") private val redisChannel: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val USER_QUEUE_DESTINATION = "/queue/status/response"
    }

    /**
     * Broadcast originated locally (event listener). Publishes to Redis so
     * peer pods can deliver to their users, **and** delivers locally for
     * users connected to this pod.
     */
    @Async("wsBroadcastExecutor")
    open fun broadcastTenantStatus(tenantId: Long) {
        publishToRedis(tenantId)
        deliverLocally(tenantId)
    }

    /**
     * Broadcast forwarded from another pod via Redis. Delivers locally only,
     * never re-publishes — that would loop.
     */
    @Async("wsBroadcastExecutor")
    open fun deliverLocallyOnly(tenantId: Long) {
        deliverLocally(tenantId)
    }

    private fun publishToRedis(tenantId: Long) {
        try {
            redisTemplate.convertAndSend(redisChannel, tenantId)
        } catch (e: Exception) {
            logger.warn("Redis publish failed for tenantId={} channel={}: {}",
                tenantId, redisChannel, e.message)
        }
    }

    private fun deliverLocally(tenantId: Long) {
        val targetUsers = try {
            resolveLocalRecipients(tenantId)
        } catch (e: Exception) {
            logger.warn("Failed to resolve recipients for tenantId={}: {}", tenantId, e.message)
            return
        }
        if (targetUsers.isEmpty()) {
            logger.trace("No local recipients for tenantId={}", tenantId)
            return
        }

        val snapshot = try {
            assembler.assembleStatusForTenant(tenantId)
        } catch (e: Exception) {
            logger.error("Failed to assemble snapshot for tenantId={}: {}", tenantId, e.message, e)
            return
        }

        var delivered = 0
        targetUsers.forEach { username ->
            try {
                messagingTemplate.convertAndSendToUser(username, USER_QUEUE_DESTINATION, snapshot)
                delivered++
            } catch (e: Exception) {
                logger.warn("convertAndSendToUser failed user={} tenantId={}: {}",
                    username, tenantId, e.message)
            }
        }
        logger.info("WS broadcast tenantId={} usersTargeted={} delivered={} tenants-in-payload={}",
            tenantId, targetUsers.size, delivered, snapshot.tenants.size)
    }

    /**
     * Active users of this tenant ∩ users with at least one STOMP session
     * on this pod. The intersection avoids serialisation work for offline
     * users (their `convertAndSendToUser` would be a no-op anyway, but a
     * costly one — Spring still serialises and resolves the destination).
     *
     * **Identity convention**: Spring Security maps each `User.email` to
     * `userDetails.username` (see [CustomUserDetailsService]). The JWT
     * carries the email as subject; [com.apptolast.invernaderos.config
     * .StompJwtAuthInterceptor] uses that as the STOMP session principal
     * name. We therefore key recipients by **email**, not by the optional
     * `User.username` display name.
     */
    private fun resolveLocalRecipients(tenantId: Long): Set<String> {
        val activeTenantEmails = userRepository.findByTenantIdAndIsActiveTrue(tenantId)
            .map { it.email }
            .toSet()
        if (activeTenantEmails.isEmpty()) return emptySet()

        val connectedPrincipalNames = simpUserRegistry.users.mapNotNull { it.name }.toSet()
        return activeTenantEmails intersect connectedPrincipalNames
    }
}
