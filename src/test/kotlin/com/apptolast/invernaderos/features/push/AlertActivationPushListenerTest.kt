package com.apptolast.invernaderos.features.push

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedEvent
import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.AlertSeverityRepository
import com.apptolast.invernaderos.features.push.infrastructure.adapter.output.AlertActivationPushListener
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.sector.SectorRepository
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional

/**
 * Unit tests for AlertActivationPushListener — MockK style, no Spring context.
 */
class AlertActivationPushListenerTest {

    private lateinit var fcmPushService: FcmPushService
    private lateinit var alertSeverityRepository: AlertSeverityRepository
    private lateinit var sectorRepository: SectorRepository
    private lateinit var listener: AlertActivationPushListener

    @BeforeEach
    fun setup() {
        fcmPushService = mockk(relaxed = true)
        alertSeverityRepository = mockk()
        sectorRepository = mockk()
        listener = AlertActivationPushListener(
            fcmPushService,
            alertSeverityRepository,
            sectorRepository
        )
    }

    private fun alert(
        isResolved: Boolean = false,
        severityId: Short? = 4,
        message: String? = "Sensor offline"
    ): Alert = Alert(
        id = 1L,
        code = "ALT-00010",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = "SCT-00020",
        alertTypeId = 1,
        alertTypeName = "SENSOR_OFFLINE",
        severityId = severityId,
        severityName = "CRITICAL",
        severityLevel = 4,
        message = message,
        description = null,
        clientName = null,
        isResolved = isResolved,
        resolvedAt = if (isResolved) Instant.now() else null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private fun change(toResolved: Boolean): AlertStateChange = AlertStateChange(
        id = 1L,
        alertId = 1L,
        fromResolved = !toResolved,
        toResolved = toResolved,
        source = AlertSignalSource.MQTT,
        rawValue = if (toResolved) "0" else "1",
        at = Instant.now()
    )

    private fun severity(notifyPush: Boolean = true): AlertSeverity = AlertSeverity(
        id = 4,
        name = "CRITICAL",
        level = 4,
        description = null,
        color = "#FF0000",
        requiresAction = true,
        notificationDelayMinutes = 0,
        notifyPush = notifyPush
    )

    private fun sector(): Sector = mockk(relaxed = true) {
        every { id } returns 20L
        every { greenhouseId } returns 100L
    }

    @Test
    fun `should send push when alert is activated and severity allows it`() {
        every { alertSeverityRepository.findById(4) } returns Optional.of(severity())
        every { sectorRepository.findById(20L) } returns Optional.of(sector())
        justRun { fcmPushService.sendAlertToTenant(any()) }

        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(), change = change(toResolved = false))
        )

        verify(exactly = 1) {
            fcmPushService.sendAlertToTenant(match {
                it.alertCode == "ALT-00010" &&
                    it.tenantId == 10L &&
                    it.greenhouseId == 100L &&
                    it.sectorId == 20L &&
                    it.severityName == "CRITICAL"
            })
        }
    }

    @Test
    fun `should NOT send push when alert is resolved (toResolved=true)`() {
        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(isResolved = true), change = change(toResolved = true))
        )

        verify(exactly = 0) { fcmPushService.sendAlertToTenant(any()) }
    }

    @Test
    fun `should NOT send push when severity has notify_push=false`() {
        every { alertSeverityRepository.findById(4) } returns Optional.of(severity(notifyPush = false))

        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(), change = change(toResolved = false))
        )

        verify(exactly = 0) { fcmPushService.sendAlertToTenant(any()) }
    }

    @Test
    fun `should NOT send push when alert has null severityId`() {
        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(severityId = null), change = change(toResolved = false))
        )

        verify(exactly = 0) { fcmPushService.sendAlertToTenant(any()) }
    }

    @Test
    fun `should NOT send push when severity is unknown`() {
        every { alertSeverityRepository.findById(4) } returns Optional.empty()

        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(), change = change(toResolved = false))
        )

        verify(exactly = 0) { fcmPushService.sendAlertToTenant(any()) }
    }

    @Test
    fun `should NOT send push when sector is unknown`() {
        every { alertSeverityRepository.findById(4) } returns Optional.of(severity())
        every { sectorRepository.findById(20L) } returns Optional.empty()

        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(), change = change(toResolved = false))
        )

        verify(exactly = 0) { fcmPushService.sendAlertToTenant(any()) }
    }

    @Test
    fun `should fall back to alert code when message and description are null`() {
        every { alertSeverityRepository.findById(4) } returns Optional.of(severity())
        every { sectorRepository.findById(20L) } returns Optional.of(sector())
        justRun { fcmPushService.sendAlertToTenant(any()) }

        listener.onAlertActivated(
            AlertStateChangedEvent(
                alert = alert(message = null),
                change = change(toResolved = false)
            )
        )

        verify(exactly = 1) {
            fcmPushService.sendAlertToTenant(match { it.body == "ALT-00010" })
        }
    }

    @Test
    fun `should swallow exceptions from FCM service (no propagation)`() {
        every { alertSeverityRepository.findById(4) } returns Optional.of(severity())
        every { sectorRepository.findById(20L) } returns Optional.of(sector())
        every { fcmPushService.sendAlertToTenant(any()) } throws RuntimeException("FCM down")

        // Must not throw — the listener catches and logs.
        listener.onAlertActivated(
            AlertStateChangedEvent(alert = alert(), change = change(toResolved = false))
        )

        verify(exactly = 1) { fcmPushService.sendAlertToTenant(any()) }
    }
}
