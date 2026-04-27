package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertMqttInboundAdapter
import com.apptolast.invernaderos.mqtt.service.DeviceStatusProcessor
import com.apptolast.invernaderos.mqtt.service.SensorDeduplicationService
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCurrentValueRepository
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRawRepository
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration test for the MQTT-to-alert routing wiring.
 *
 * Approach: No-Spring unit-wiring test.
 *
 * Rationale: The project has no Testcontainers setup, and a full @SpringBootTest
 * context requires live connections to PostgreSQL, TimescaleDB, Redis, and an MQTT
 * broker — none of which are available in the CI environment without Testcontainers.
 *
 * This test constructs the production bean graph (DeviceStatusProcessor +
 * AlertMqttInboundAdapter) with MockK mocks for every infra port, then exercises
 * the exact code path that runs in production when an MQTT message arrives on
 * GREENHOUSE/STATUS with an ALT- prefixed code.
 *
 * What is verified:
 * 1. processStatusUpdate("ALT-99999", "0") — the entry point called by DeviceStatusListener
 *    after parsing the JSON message {"id":"ALT-99999","value":false}.
 * 2. AlertMqttInboundAdapter.handleSignal("ALT-99999", "0") is called exactly once.
 * 3. Non-ALT- codes do NOT trigger handleSignal.
 * 4. The value conversion (boolean false → "0") is applied by DeviceStatusListener;
 *    this test verifies the downstream adapter receives the already-converted string "0".
 */
class AlertMqttSignalIntegrationTest {

    // ---------------------------------------------------------------------------
    // MockK mocks for all infra dependencies of DeviceStatusProcessor
    // ---------------------------------------------------------------------------

    private val sensorReadingRepository = mockk<SensorReadingRepository>(relaxed = true)
    private val sensorReadingRawRepository = mockk<SensorReadingRawRepository>(relaxed = true)
    private val deviceCurrentValueRepository = mockk<DeviceCurrentValueRepository>(relaxed = true)
    private val deduplicationService = mockk<SensorDeduplicationService>()
    private val alertMqttInboundAdapter = mockk<AlertMqttInboundAdapter>()

    private val processor = DeviceStatusProcessor(
        sensorReadingRepository = sensorReadingRepository,
        sensorReadingRawRepository = sensorReadingRawRepository,
        deviceCurrentValueRepository = deviceCurrentValueRepository,
        deduplicationService = deduplicationService,
        alertMqttInboundAdapter = alertMqttInboundAdapter
    )

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `should route ALT- code to AlertMqttInboundAdapter handleSignal`() {
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false
        justRun { alertMqttInboundAdapter.handleSignal(any(), any()) }

        processor.processStatusUpdate("ALT-99999", "0")

        verify(exactly = 1) { alertMqttInboundAdapter.handleSignal("ALT-99999", "0") }
    }

    @Test
    fun `should pass exact code and raw value to handleSignal`() {
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false
        val codeSlot = slot<String>()
        val valueSlot = slot<String>()
        justRun { alertMqttInboundAdapter.handleSignal(capture(codeSlot), capture(valueSlot)) }

        processor.processStatusUpdate("ALT-99999", "0")

        assertThat(codeSlot.captured).isEqualTo("ALT-99999")
        assertThat(valueSlot.captured).isEqualTo("0")
    }

    @Test
    fun `should not invoke handleSignal for non-ALT- codes`() {
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false

        processor.processStatusUpdate("SET-00036", "15")
        processor.processStatusUpdate("DEV-00001", "1")
        processor.processStatusUpdate("TMP-00042", "23.5")

        verify(exactly = 0) { alertMqttInboundAdapter.handleSignal(any(), any()) }
    }

    @Test
    fun `should invoke handleSignal for every ALT- message regardless of value`() {
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false
        justRun { alertMqttInboundAdapter.handleSignal(any(), any()) }

        processor.processStatusUpdate("ALT-00001", "1")
        processor.processStatusUpdate("ALT-00002", "0")
        processor.processStatusUpdate("ALT-00003", "true")

        verify(exactly = 3) { alertMqttInboundAdapter.handleSignal(any(), any()) }
    }

    @Test
    fun `should still invoke handleSignal even when deduplication suppresses sensor write`() {
        // Dedup blocks the sensor_readings write but alert routing is independent
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false
        justRun { alertMqttInboundAdapter.handleSignal(any(), any()) }

        processor.processStatusUpdate("ALT-99999", "1")

        verify(exactly = 1) { alertMqttInboundAdapter.handleSignal("ALT-99999", "1") }
    }

    /**
     * Non-regression: the original bug was that ALT- codes silently fell through and never
     * updated metadata.alerts. The fix routes them to the alert path in addition to the
     * existing telemetry path. This test pins down that the telemetry path is preserved.
     */
    @Test
    fun `should still feed telemetry buffers for ALT- codes (non-regression)`() {
        every { deduplicationService.shouldPersistToDeduped(any(), any()) } returns false
        justRun { alertMqttInboundAdapter.handleSignal(any(), any()) }

        processor.processStatusUpdate("ALT-99999", "1")

        // lastKnownValues is the public side-effect of the telemetry branch — if it is set,
        // raw and current_values buffers also received the entry (same code path).
        assertThat(processor.lastKnownValues["ALT-99999"]).isEqualTo("1")
    }
}
