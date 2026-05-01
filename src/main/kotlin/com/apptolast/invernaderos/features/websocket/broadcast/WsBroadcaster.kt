package com.apptolast.invernaderos.features.websocket.broadcast

import com.apptolast.invernaderos.features.user.UserRepository
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusAssembler
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

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
    private val objectMapper: ObjectMapper,
    @Qualifier("redisTemplate") private val redisTemplate: RedisTemplate<String, Any>,
    @Qualifier("wsBroadcastChannel") private val redisChannel: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Last broadcast timestamp (System.nanoTime) per tenant, used to compute
     * how often we are pushing to clients of a given tenant. Useful to verify
     * that the natural rate matches the configured `flushPendingChanges`
     * cadence (≤1 broadcast/s/tenant) and to detect runaway producers.
     *
     * Map grows at most by the number of active tenants. No invalidation
     * needed — entries are just stale timestamps that get overwritten on
     * the next broadcast.
     */
    private val lastBroadcastNanos = ConcurrentHashMap<Long, Long>()

    companion object {
        const val USER_QUEUE_DESTINATION = "/queue/status/response"
        const val SOURCE_REDIS_PEER = "REDIS_PEER"
    }

    /**
     * Broadcast originated locally (event listener). Publishes to Redis so
     * peer pods can deliver to their users, **and** delivers locally for
     * users connected to this pod.
     *
     * @param source human-readable origin tag for logs/observability —
     *   `SENSOR_FLUSH`, `ALERT_ACTIVATED`, `ALERT_RESOLVED`, `GREENHOUSE_CRUD`,
     *   `SECTOR_CRUD`, `DEVICE_CRUD`, `SETTING_CRUD`, `USER_CRUD`. Not
     *   transmitted over Redis — peers see [SOURCE_REDIS_PEER] instead.
     */
    @Async("wsBroadcastExecutor")
    open fun broadcastTenantStatus(tenantId: Long, source: String) {
        publishToRedis(tenantId)
        deliverLocally(tenantId, source)
    }

    /**
     * Broadcast forwarded from another pod via Redis. Delivers locally only,
     * never re-publishes — that would loop.
     */
    @Async("wsBroadcastExecutor")
    open fun deliverLocallyOnly(tenantId: Long) {
        deliverLocally(tenantId, SOURCE_REDIS_PEER)
    }

    private fun publishToRedis(tenantId: Long) {
        try {
            redisTemplate.convertAndSend(redisChannel, tenantId)
        } catch (e: Exception) {
            logger.warn("Redis publish failed for tenantId={} channel={}: {}",
                tenantId, redisChannel, e.message)
        }
    }

    private fun deliverLocally(tenantId: Long, source: String) {
        val startNanos = System.nanoTime()
        val targetUsers = try {
            resolveLocalRecipients(tenantId)
        } catch (e: Exception) {
            logger.warn("Failed to resolve recipients for tenantId={}: {}", tenantId, e.message)
            return
        }
        if (targetUsers.isEmpty()) {
            // Don't log at INFO — broadcasts to tenants whose users live on
            // another pod are normal and expected. The peer pod logs the
            // delivery for them.
            logger.debug("No local recipients for tenantId={} source={}", tenantId, source)
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

        val deltaMs = updateLastBroadcastAndComputeDelta(tenantId, startNanos)
        val payloadBytes = safePayloadSize(snapshot)
        val counts = countSnapshotItems(snapshot)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        // Single-line INFO log per broadcast. Format optimised so each field
        // is greppable (`grep "WS broadcast" | grep "source=SENSOR_FLUSH"`).
        logger.info(
            "WS broadcast tenantId={} source={} usersTargeted={} delivered={} " +
                "payloadBytes={} deltaMsSinceLastForTenant={} " +
                "greenhouses={} sectors={} devices={} settings={} alerts={} elapsedMs={}",
            tenantId, source, targetUsers.size, delivered,
            payloadBytes, deltaMs,
            counts.greenhouses, counts.sectors, counts.devices, counts.settings, counts.alerts,
            elapsedMs
        )
    }

    private fun updateLastBroadcastAndComputeDelta(tenantId: Long, nowNanos: Long): Long {
        val previous = lastBroadcastNanos.put(tenantId, nowNanos)
        return if (previous == null) -1L else (nowNanos - previous) / 1_000_000
    }

    /**
     * Bytes the JSON-serialised snapshot would occupy on the wire. Computed
     * with the same Jackson [ObjectMapper] used by Spring for the actual
     * STOMP delivery, so the number is representative.
     *
     * Falls back to `-1` if serialisation fails — the broadcast itself isn't
     * affected because Spring re-serialises with its MessageConverter.
     */
    private fun safePayloadSize(snapshot: GreenhouseStatusResponse): Int = try {
        objectMapper.writeValueAsBytes(snapshot).size
    } catch (e: Exception) {
        logger.warn("Failed to size snapshot payload: {}", e.message)
        -1
    }

    private data class SnapshotCounts(
        val greenhouses: Int,
        val sectors: Int,
        val devices: Int,
        val settings: Int,
        val alerts: Int
    )

    private fun countSnapshotItems(snapshot: GreenhouseStatusResponse): SnapshotCounts {
        var gh = 0; var sec = 0; var dev = 0; var set = 0; var alt = 0
        snapshot.tenants.forEach { tenant ->
            tenant.greenhouses.forEach { greenhouse ->
                gh++
                greenhouse.sectors.forEach { sector ->
                    sec++
                    dev += sector.devices.size
                    set += sector.settings.size
                    alt += sector.alerts.size
                }
            }
        }
        return SnapshotCounts(greenhouses = gh, sectors = sec, devices = dev, settings = set, alerts = alt)
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
