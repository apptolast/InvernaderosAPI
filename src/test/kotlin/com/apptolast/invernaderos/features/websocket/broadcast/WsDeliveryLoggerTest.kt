package com.apptolast.invernaderos.features.websocket.broadcast

import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pure tests for the delivery logger. We don't assert on the SLF4J output
 * directly (would require attaching a test appender and is brittle); instead
 * we test the deterministic helpers — sha256 hex and the truncation logic —
 * which are the parts that affect log correctness, and exercise the public
 * `logDelivery` to ensure it never throws on a normal snapshot.
 */
class WsDeliveryLoggerTest {

    private val mapper = ObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `sha256 hex is deterministic and 64 chars long`() {
        val logger = WsDeliveryLogger(mapper, logPayload = false, maxBytes = 1024)
        val bytes = """{"hello":"world"}""".toByteArray()

        val a = logger.sha256Hex(bytes)
        val b = logger.sha256Hex(bytes)

        assertThat(a).isEqualTo(b)
        assertThat(a).hasSize(64)
        assertThat(a).matches("^[0-9a-f]+$")
    }

    @Test
    fun `sha256 differs for different payloads`() {
        val logger = WsDeliveryLogger(mapper, logPayload = false, maxBytes = 1024)
        val a = logger.sha256Hex("foo".toByteArray())
        val b = logger.sha256Hex("bar".toByteArray())
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `truncate returns full string when bytes fit under max`() {
        val logger = WsDeliveryLogger(mapper, logPayload = false, maxBytes = 1024)
        val (preview, dropped) = logger.truncate("abc".toByteArray(), 1024)
        assertThat(preview).isEqualTo("abc")
        assertThat(dropped).isZero
    }

    @Test
    fun `truncate slices at max and reports dropped byte count`() {
        val logger = WsDeliveryLogger(mapper, logPayload = false, maxBytes = 4)
        val (preview, dropped) = logger.truncate("abcdefghi".toByteArray(), 4)
        assertThat(preview).isEqualTo("abcd")
        assertThat(dropped).isEqualTo(5)
    }

    @Test
    fun `logDelivery does not throw on minimal snapshot`() {
        val logger = WsDeliveryLogger(mapper, logPayload = true, maxBytes = 8192)
        val snapshot = GreenhouseStatusResponse(
            timestamp = Instant.parse("2026-05-04T22:15:00Z"),
            tenants = emptyList(),
        )
        // Sanity: no exception means the serialize-and-hash path is wired up.
        logger.logDelivery(
            principal = "alice@example.com",
            tenantIds = emptyList(),
            source = "INITIAL_REQUEST",
            snapshot = snapshot,
            sessionId = "s1",
        )
    }
}
