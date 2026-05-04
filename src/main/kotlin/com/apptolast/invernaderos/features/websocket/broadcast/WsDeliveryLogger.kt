package com.apptolast.invernaderos.features.websocket.broadcast

import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * Per-recipient observability for the greenhouse-status WebSocket.
 *
 * Two log lines per delivery:
 *
 * 1. **Header** (always, INFO): structured one-liner with principal,
 *    tenantId, source, payload size, sha256 hash, and item counts. Lets us
 *    answer "what did user X see at time T?" without enabling payload dumps.
 * 2. **Payload** (gated by `invernaderos.websocket.log-payload`, INFO with
 *    marker `WS_PAYLOAD`): the full JSON snapshot, truncated to
 *    `invernaderos.websocket.log-payload-max-bytes`. ON in dev, OFF in prod
 *    by default to avoid GB of log volume.
 *
 * The sha256 lets us correlate against client-side logs (which already log
 * received bytes) without exposing the JSON in prod.
 */
@Component
class WsDeliveryLogger(
    private val objectMapper: ObjectMapper,
    @Value("\${invernaderos.websocket.log-payload:false}") private val logPayload: Boolean,
    @Value("\${invernaderos.websocket.log-payload-max-bytes:16384}") private val maxBytes: Int,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Logs a delivery for a single recipient (broadcast or initial request).
     *
     * @param principal user email (matches [com.apptolast.invernaderos.features.user.User.email]).
     * @param tenantIds tenants present in the snapshot — used to detect any
     *   regression where a user receives data outside their own tenant.
     * @param source human-readable origin tag (`SENSOR_FLUSH`, `INITIAL_REQUEST`, ...).
     * @param sessionId optional STOMP session id for correlation with the
     *   `STOMP CONNECT accepted ... sessionId=...` line.
     */
    fun logDelivery(
        principal: String,
        tenantIds: List<Long>,
        source: String,
        snapshot: GreenhouseStatusResponse,
        sessionId: String? = null,
    ) {
        val bytes = serializeSafely(snapshot)
        val sha = if (bytes != null) sha256Hex(bytes) else "n/a"
        val size = bytes?.size ?: -1
        val counts = countItems(snapshot)

        logger.info(
            "WS delivery principal={} sessionId={} source={} tenantIds={} payloadBytes={} " +
                "payloadSha256={} greenhouses={} sectors={} devices={} settings={} alerts={}",
            principal, sessionId ?: "?", source, tenantIds, size, sha,
            counts.greenhouses, counts.sectors, counts.devices, counts.settings, counts.alerts,
        )

        if (logPayload && bytes != null) {
            val (preview, truncated) = truncate(bytes, maxBytes)
            logger.info(
                "WS_PAYLOAD principal={} sessionId={} source={} sha256={} bytes={} truncatedBytes={} json={}",
                principal, sessionId ?: "?", source, sha, size, truncated, preview,
            )
        }
    }

    private fun serializeSafely(snapshot: GreenhouseStatusResponse): ByteArray? = try {
        objectMapper.writeValueAsBytes(snapshot)
    } catch (e: Exception) {
        logger.warn("WS delivery: failed to serialize snapshot for logging: {}", e.message)
        null
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns (preview, truncatedBytes). preview is UTF-8 decoded from the
     * first [maxBytes] bytes; truncatedBytes is how many bytes were dropped.
     */
    internal fun truncate(bytes: ByteArray, maxBytes: Int): Pair<String, Int> {
        if (bytes.size <= maxBytes) return String(bytes, Charsets.UTF_8) to 0
        // Slice on byte boundary; UTF-8 multibyte chars at the cut may show
        // as replacement chars — acceptable for log diagnostics.
        val head = String(bytes.copyOf(maxBytes), Charsets.UTF_8)
        return head to (bytes.size - maxBytes)
    }

    private data class Counts(
        val greenhouses: Int,
        val sectors: Int,
        val devices: Int,
        val settings: Int,
        val alerts: Int,
    )

    private fun countItems(snapshot: GreenhouseStatusResponse): Counts {
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
        return Counts(gh, sec, dev, set, alt)
    }
}
