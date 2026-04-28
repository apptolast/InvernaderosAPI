package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertEchoPublisherPort
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedEvent
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedMqttEchoListener
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for the MQTT echo listener — MockK style, no Spring context.
 *
 * Covers:
 *  - Value mapping for both ValueTrueMeans modes (ACTIVE/RESOLVED) on both transitions.
 *  - Echo published for every source (API, MQTT, SYSTEM) — Option B.
 *  - Kill switch (echo.enabled=false) short-circuits.
 *  - Defensive filter for non-transition events.
 *  - Exception in echoPublisher is swallowed (never propagates to Spring's
 *    transaction event multicaster).
 */
class AlertStateChangedMqttEchoListenerTest {

    private val echoPublisher = mockk<AlertEchoPublisherPort>()

    private fun listener(
        valueTrueMeans: AlertMqttProperties.ValueTrueMeans = AlertMqttProperties.ValueTrueMeans.ACTIVE,
        echoEnabled: Boolean = true
    ): AlertStateChangedMqttEchoListener {
        val props = AlertMqttProperties(
            valueTrueMeans = valueTrueMeans,
            echo = AlertMqttProperties.Echo(enabled = echoEnabled)
        )
        return AlertStateChangedMqttEchoListener(echoPublisher, props)
    }

    private fun alert(code: String = "ALT-00001", isResolved: Boolean): Alert = Alert(
        id = 1L,
        code = code,
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        message = null,
        description = null,
        clientName = null,
        isResolved = isResolved,
        resolvedAt = if (isResolved) Instant.now() else null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private fun change(
        from: Boolean,
        to: Boolean,
        source: AlertSignalSource = AlertSignalSource.API,
        rawValue: String? = null
    ): AlertStateChange = AlertStateChange(
        id = 1L,
        alertId = 1L,
        fromResolved = from,
        toResolved = to,
        source = source,
        rawValue = rawValue,
        at = Instant.now()
    )

    // ---------------------------------------------------------------------------
    // Value mapping — ACTIVE mode
    // ---------------------------------------------------------------------------

    @Test
    fun `ACTIVE mode - alert becomes ACTIVE (isResolved=false) publishes value=1`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = false),
            change = change(from = true, to = false)
        )

        listener(AlertMqttProperties.ValueTrueMeans.ACTIVE).onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-00001", 1) }
    }

    @Test
    fun `ACTIVE mode - alert becomes RESOLVED (isResolved=true) publishes value=0`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true)
        )

        listener(AlertMqttProperties.ValueTrueMeans.ACTIVE).onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-00001", 0) }
    }

    // ---------------------------------------------------------------------------
    // Value mapping — RESOLVED mode (inverse)
    // ---------------------------------------------------------------------------

    @Test
    fun `RESOLVED mode - alert becomes ACTIVE (isResolved=false) publishes value=0`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = false),
            change = change(from = true, to = false)
        )

        listener(AlertMqttProperties.ValueTrueMeans.RESOLVED).onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-00001", 0) }
    }

    @Test
    fun `RESOLVED mode - alert becomes RESOLVED (isResolved=true) publishes value=1`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true)
        )

        listener(AlertMqttProperties.ValueTrueMeans.RESOLVED).onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-00001", 1) }
    }

    // ---------------------------------------------------------------------------
    // Source coverage — Option B: echo always
    // ---------------------------------------------------------------------------

    @Test
    fun `should publish echo for source=API (frontend-driven)`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true, source = AlertSignalSource.API)
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish(any(), any()) }
    }

    @Test
    fun `should publish echo for source=MQTT (Option B - echo always)`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true, source = AlertSignalSource.MQTT, rawValue = "1")
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish(any(), any()) }
    }

    @Test
    fun `should publish echo for source=SYSTEM`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = false),
            change = change(from = true, to = false, source = AlertSignalSource.SYSTEM)
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish(any(), any()) }
    }

    // ---------------------------------------------------------------------------
    // Defensive layers
    // ---------------------------------------------------------------------------

    @Test
    fun `should NOT publish when echo is disabled (kill switch)`() {
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true)
        )

        listener(echoEnabled = false).onAlertStateChanged(event)

        verify(exactly = 0) { echoPublisher.publish(any(), any()) }
    }

    @Test
    fun `should NOT publish when fromResolved equals toResolved (defensive filter)`() {
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = true, to = true)
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 0) { echoPublisher.publish(any(), any()) }
    }

    @Test
    fun `should swallow exceptions from echoPublisher (no propagation)`() {
        every { echoPublisher.publish(any(), any()) } throws RuntimeException("MQTT broker down")
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true)
        )

        // Should not throw — the listener catches and logs.
        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish(any(), any()) }
    }

    @Test
    fun `should pass exact alert code (no transformation)`() {
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(code = "ALT-99999", isResolved = false),
            change = change(from = true, to = false)
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-99999", 1) }
    }

    @Test
    fun `published value is always Int (port enforces type, not String)`() {
        // Pinning down that the listener delegates an Int to the port — wire-format
        // (numeric vs boolean) is the adapter's responsibility, not the listener's.
        justRun { echoPublisher.publish(any(), any()) }
        val event = AlertStateChangedEvent(
            alert = alert(isResolved = true),
            change = change(from = false, to = true)
        )

        listener().onAlertStateChanged(event)

        verify(exactly = 1) { echoPublisher.publish("ALT-00001", 0) }
    }
}
