package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingAlertScannerPort
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingCandidate
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class DetectAgingAlertsUseCaseImplTest {

    private val agingAlertScanner = mockk<AgingAlertScannerPort>()
    private val notificationLogRepository = mockk<NotificationLogRepositoryPort>()

    private val thresholdsConfig = AgingThresholdsConfig(
        mapOf(
            4.toShort() to Duration.ofMinutes(30),  // CRITICAL → 30 min
            3.toShort() to Duration.ofHours(2),     // ERROR → 2 h
            2.toShort() to Duration.ofHours(8)      // WARNING → 8 h
            // INFO (level 1) intentionally omitted — no aging notification
        )
    )

    private val useCase = DetectAgingAlertsUseCaseImpl(
        agingAlertScanner = agingAlertScanner,
        notificationLogRepository = notificationLogRepository,
        thresholdsConfig = thresholdsConfig
    )

    private val criticalAlert = Alert(
        id = 1L,
        code = "ALT-00001",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = 4,
        severityName = "CRITICAL",
        severityLevel = 4,
        message = "Temperatura crítica",
        description = null,
        clientName = null,
        isResolved = false,
        resolvedAt = null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T09:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T09:00:00Z")
    )

    private val lastActivationAt = Instant.parse("2026-01-01T09:00:00Z")

    private val criticalCandidate = AgingCandidate(
        alert = criticalAlert,
        lastActivationAt = lastActivationAt,
        ageMinutes = 35L,
        thresholdMinutes = 30,
        severityName = "CRITICAL"
    )

    @Test
    fun `should emit event for CRITICAL alert older than threshold`() {
        every { agingAlertScanner.scan(thresholdsConfig) } returns listOf(criticalCandidate)
        every {
            notificationLogRepository.hasRecentSent(
                notificationType = NotificationType.ALERT_AGING,
                alertId = 1L,
                sinceInstant = lastActivationAt
            )
        } returns false

        val events = useCase.detect()

        assertThat(events).hasSize(1)
        val event = events.first()
        assertThat(event.alertId).isEqualTo(1L)
        assertThat(event.alertCode).isEqualTo("ALT-00001")
        assertThat(event.severityLevel).isEqualTo(4.toShort())
        assertThat(event.ageMinutes).isEqualTo(35L)
        assertThat(event.thresholdMinutes).isEqualTo(30)
    }

    @Test
    fun `should NOT emit event when notificationLog hasRecentSent is true`() {
        every { agingAlertScanner.scan(thresholdsConfig) } returns listOf(criticalCandidate)
        every {
            notificationLogRepository.hasRecentSent(
                notificationType = NotificationType.ALERT_AGING,
                alertId = 1L,
                sinceInstant = lastActivationAt
            )
        } returns true

        val events = useCase.detect()

        assertThat(events).isEmpty()
    }

    @Test
    fun `should emit multiple events for multiple qualifying candidates`() {
        val alert2 = criticalAlert.copy(id = 2L, code = "ALT-00002", severityId = 3, severityLevel = 3, severityName = "ERROR")
        val alert3 = criticalAlert.copy(id = 3L, code = "ALT-00003", severityId = 2, severityLevel = 2, severityName = "WARNING")

        val candidate2 = criticalCandidate.copy(alert = alert2, thresholdMinutes = 120, severityName = "ERROR")
        val candidate3 = criticalCandidate.copy(alert = alert3, thresholdMinutes = 480, severityName = "WARNING")

        every { agingAlertScanner.scan(thresholdsConfig) } returns listOf(criticalCandidate, candidate2, candidate3)
        every {
            notificationLogRepository.hasRecentSent(
                notificationType = NotificationType.ALERT_AGING,
                alertId = any(),
                sinceInstant = any()
            )
        } returns false

        val events = useCase.detect()

        assertThat(events).hasSize(3)
        assertThat(events.map { it.alertCode }).containsExactlyInAnyOrder("ALT-00001", "ALT-00002", "ALT-00003")
    }

    @Test
    fun `should not call any publisher port — caller is responsible for publishing`() {
        // The use case returns the list; it MUST NOT call any event bus port itself.
        // Verified by absence of any publisher mock injected into the use case constructor.
        every { agingAlertScanner.scan(thresholdsConfig) } returns listOf(criticalCandidate)
        every {
            notificationLogRepository.hasRecentSent(any(), any(), any())
        } returns false

        val events = useCase.detect()

        // If the use case had called a publisher, compilation would fail (no publisher injected).
        // This test documents the design contract: return list, don't publish.
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(AlertAgingDetectedEvent::class.java)
    }
}
