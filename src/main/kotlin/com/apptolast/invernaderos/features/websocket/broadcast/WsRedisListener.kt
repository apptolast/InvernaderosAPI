package com.apptolast.invernaderos.features.websocket.broadcast

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

/**
 * Bridge between Redis pub/sub and the local pod's [WsBroadcaster].
 *
 * Why Redis pub/sub specifically:
 *  - Spring's `enableSimpleBroker` keeps STOMP sessions in-memory per pod.
 *    When prod runs ≥2 replicas, a `convertAndSendToUser(email, …)` only
 *    reaches the pod where that user's session lives. A broadcast triggered
 *    on the wrong pod is silently dropped.
 *  - Redis pub/sub is **fire-and-forget** with no persistence. If a pod is
 *    down when the message is published it loses that message — and that's
 *    desirable here because the snapshot is idempotent: the next sensor
 *    flush (≤1 s away) carries the up-to-date state. Streams or queues
 *    would over-engineer this with backlogs we don't want.
 *
 * Loop prevention: this listener calls [WsBroadcaster.deliverLocallyOnly]
 * (NOT `broadcastTenantStatus`) so we do not re-publish the same tenantId
 * we just received.
 */
@Component
class WsRedisListener(
    private val wsBroadcaster: WsBroadcaster
) : MessageListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val raw = String(message.body).trim().trim('"')
        val tenantId = raw.toLongOrNull() ?: run {
            logger.warn("Ignoring Redis pub/sub message with non-numeric body: '{}'", raw)
            return
        }
        // INFO so the cross-pod handover is visible in logs without enabling
        // DEBUG. Will appear at most once per broadcast on each peer pod and
        // pairs with the originating pod's "WS broadcast" line for easy
        // grepping by tenantId.
        logger.info("WS broadcast peer received tenantId={} channel={}",
            tenantId, String(message.channel))
        try {
            wsBroadcaster.deliverLocallyOnly(tenantId)
        } catch (e: Exception) {
            // Defensive: deliverLocallyOnly is itself try/catch'd, but
            // a Redis listener that throws is silently dropped from the
            // container by some Spring versions. Belt-and-suspenders.
            logger.error("WsRedisListener failed for tenantId={}: {}", tenantId, e.message, e)
        }
    }
}
