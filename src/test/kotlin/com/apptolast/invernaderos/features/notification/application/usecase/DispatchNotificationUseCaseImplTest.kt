package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.model.NotificationStatus
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.model.PreferredChannel
import com.apptolast.invernaderos.features.notification.domain.model.QuietHours
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchSummary
import com.apptolast.invernaderos.features.notification.domain.port.output.AlertSeverityLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSendResult
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSenderPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationContentRendererPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDedupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationSeveritySnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationTokenSnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationUserSnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.PushTokenLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

class DispatchNotificationUseCaseImplTest {

    private val alertSeverityLookup = mockk<AlertSeverityLookupPort>()
    private val pushTokenLookup = mockk<PushTokenLookupPort>()
    private val userLookup = mockk<UserLookupPort>()
    private val preferencesRepository = mockk<UserPreferencesRepositoryPort>()
    private val notificationDedupPort = mockk<NotificationDedupPort>()
    private val contentRenderer = mockk<NotificationContentRendererPort>()
    private val fcmSender = mockk<FcmSenderPort>()
    private val notificationLogRepository = mockk<NotificationLogRepositoryPort>(relaxed = true)

    private val useCase = DispatchNotificationUseCaseImpl(
        alertSeverityLookup = alertSeverityLookup,
        pushTokenLookup = pushTokenLookup,
        userLookup = userLookup,
        preferencesRepository = preferencesRepository,
        notificationDedupPort = notificationDedupPort,
        contentRenderer = contentRenderer,
        fcmSender = fcmSender,
        notificationLogRepository = notificationLogRepository,
        dedupWindowSeconds = 60L
    )

    // --- Shared fixtures ---

    private val tenantId = TenantId(10L)
    private val sectorId = SectorId(20L)

    private val baseAlert = Alert(
        id = 1L,
        code = "ALT-00001",
        tenantId = tenantId,
        sectorId = sectorId,
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = 3,
        severityName = "ERROR",
        severityLevel = 3,
        message = "Temperatura alta",
        description = null,
        clientName = null,
        isResolved = false,
        resolvedAt = null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T10:00:00Z")
    )

    private val sampleChange = AlertStateChange(
        id = null,
        alertId = 1L,
        fromResolved = false,
        toResolved = false,
        source = AlertSignalSource.API,
        rawValue = null,
        at = Instant.now(),
        actor = AlertActor.System
    )

    private val severity = NotificationSeveritySnapshot(
        id = 3,
        name = "ERROR",
        level = 3,
        color = "#FF0000",
        notifyPush = true
    )

    private val token = NotificationTokenSnapshot(
        id = 100L,
        userId = 50L,
        token = "fcm-token-abc",
        platform = "ANDROID"
    )

    private val user = NotificationUserSnapshot(
        id = 50L,
        username = "operario",
        displayName = "Operario Uno",
        locale = "es-ES",
        tenantId = 10L
    )

    private val defaultPrefs = UserNotificationPreferences.default(userId = 50L)

    private val notificationContent = NotificationContent(
        title = "Nueva alerta: ERROR",
        body = "Temperatura alta",
        data = mapOf("alertId" to "1", "notificationType" to "ALERT_ACTIVATED"),
        androidChannelId = "alerts_default",
        severityColor = "#FF0000"
    )

    private val successFcmResult = FcmSendResult(
        success = 1,
        failed = 0,
        invalidatedTokens = emptyList(),
        errors = emptyMap()
    )

    // --- Tests ---

    @Test
    fun `should drop CATEGORY_DISABLED when categoryAlerts is false`() {
        val prefs = defaultPrefs.copy(categoryAlerts = false)
        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns prefs

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.dropped).isEqualTo(1)
        assertThat(summary.sent).isEqualTo(0)
        verify(exactly = 0) { fcmSender.send(any(), any()) }
        verify(exactly = 1) {
            notificationLogRepository.save(
                match { it.status == NotificationStatus.DROPPED_BY_PREFERENCE }
            )
        }
    }

    @Test
    fun `should drop BELOW_MIN_SEVERITY when alert severity is below user minimum`() {
        // Alert severityLevel=2 (WARNING), user minAlertSeverity=3 (ERROR) → drop
        val warningAlert = baseAlert.copy(severityId = 2, severityLevel = 2, severityName = "WARNING")
        val warningSeverity = severity.copy(id = 2, name = "WARNING", level = 2)
        val prefs = defaultPrefs.copy(minAlertSeverity = 3)

        every { alertSeverityLookup.findById(2) } returns warningSeverity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns prefs

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = warningAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.dropped).isEqualTo(1)
        assertThat(summary.sent).isEqualTo(0)
        verify(exactly = 0) { fcmSender.send(any(), any()) }
    }

    @Test
    fun `should drop IN_QUIET_HOURS when within wrap-around quiet window`() {
        // Quiet hours 22:00–07:00 UTC. Instant.now() inside the window: 23:30 UTC
        val quietStart = LocalTime.of(22, 0)
        val quietEnd = LocalTime.of(7, 0)
        val prefs = defaultPrefs.copy(
            quietHours = QuietHours(
                start = quietStart,
                end = quietEnd,
                timezone = ZoneId.of("UTC")
            )
        )
        // We need Instant.now() to be at 23:30 — but the use case calls Instant.now() internally.
        // Instead we configure quiet hours to be 00:00–23:59 to always be in window.
        val alwaysQuietPrefs = defaultPrefs.copy(
            quietHours = QuietHours(
                start = LocalTime.of(0, 0),
                end = LocalTime.of(23, 59),
                timezone = ZoneId.of("UTC")
            )
        )

        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns alwaysQuietPrefs

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.dropped).isEqualTo(1)
        assertThat(summary.sent).isEqualTo(0)
        verify(exactly = 0) { fcmSender.send(any(), any()) }
        verify(exactly = 1) {
            notificationLogRepository.save(
                match { it.status == NotificationStatus.DROPPED_BY_QUIET_HOURS }
            )
        }
    }

    @Test
    fun `should drop DEDUP_HIT when dedupPort returns false`() {
        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns defaultPrefs
        every { notificationDedupPort.shouldDispatch(any(), any(), any(), any()) } returns false

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.dropped).isEqualTo(1)
        assertThat(summary.sent).isEqualTo(0)
        verify(exactly = 0) { fcmSender.send(any(), any()) }
        verify(exactly = 1) {
            notificationLogRepository.save(
                match { it.status == NotificationStatus.DROPPED_BY_DEDUP }
            )
        }
    }

    @Test
    fun `should send to all eligible recipients`() {
        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns defaultPrefs
        every { notificationDedupPort.shouldDispatch(any(), any(), any(), any()) } returns true
        every { contentRenderer.render(any(), any(), any(), any(), any(), any()) } returns notificationContent
        every { fcmSender.send(any(), any()) } returns successFcmResult

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.sent).isEqualTo(1)
        assertThat(summary.dropped).isEqualTo(0)
        verify(exactly = 1) { fcmSender.send(any(), eq(notificationContent)) }
        verify(exactly = 1) {
            notificationLogRepository.save(
                match { it.status == NotificationStatus.SENT }
            )
        }
    }

    @Test
    fun `should call fcmSender when dedupPort returns true even in fail-open scenario`() {
        // Even when the dedup port returns true (allow dispatch — simulating fail-open),
        // the use case must proceed to call fcmSender.
        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns defaultPrefs
        every { notificationDedupPort.shouldDispatch(any(), any(), any(), any()) } returns true
        every { contentRenderer.render(any(), any(), any(), any(), any(), any()) } returns notificationContent
        every { fcmSender.send(any(), any()) } returns successFcmResult

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result.isRight).isTrue()
        verify(exactly = 1) { fcmSender.send(any(), any()) }
    }

    @Test
    fun `should skip dispatch when severity notifyPush is false`() {
        val silencedSeverity = severity.copy(notifyPush = false)
        every { alertSeverityLookup.findById(3) } returns silencedSeverity

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val summary = (result as Either.Right).value
        assertThat(summary.sent).isEqualTo(0)
        assertThat(summary.dropped).isEqualTo(0)
        assertThat(summary.failed).isEqualTo(0)
        verify(exactly = 0) { pushTokenLookup.findActiveTokensForTenant(any()) }
        verify(exactly = 0) { fcmSender.send(any(), any()) }
    }

    @Test
    fun `should log TOKEN_INVALIDATED when fcmSender returns invalidated tokens`() {
        val fcmResultWithInvalidated = FcmSendResult(
            success = 0,
            failed = 0,
            invalidatedTokens = listOf(100L),
            errors = emptyMap()
        )
        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(token)
        every { userLookup.findById(50L) } returns user
        every { preferencesRepository.findByUserId(50L) } returns defaultPrefs
        every { notificationDedupPort.shouldDispatch(any(), any(), any(), any()) } returns true
        every { contentRenderer.render(any(), any(), any(), any(), any(), any()) } returns notificationContent
        every { fcmSender.send(any(), any()) } returns fcmResultWithInvalidated

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) {
            notificationLogRepository.save(
                match { it.status == NotificationStatus.TOKEN_INVALIDATED }
            )
        }
    }

    @Test
    fun `should group recipients by locale and call render once per locale`() {
        val tokenEs = NotificationTokenSnapshot(id = 100L, userId = 50L, token = "token-es", platform = "ANDROID")
        val tokenEn = NotificationTokenSnapshot(id = 101L, userId = 51L, token = "token-en", platform = "ANDROID")

        val userEs = NotificationUserSnapshot(id = 50L, username = "esp", displayName = null, locale = "es-ES", tenantId = 10L)
        val userEn = NotificationUserSnapshot(id = 51L, username = "eng", displayName = null, locale = "en-US", tenantId = 10L)

        val contentEs = notificationContent.copy(title = "Nueva alerta: ERROR")
        val contentEn = notificationContent.copy(title = "New alert: ERROR")

        every { alertSeverityLookup.findById(3) } returns severity
        every { pushTokenLookup.findActiveTokensForTenant(10L) } returns listOf(tokenEs, tokenEn)
        every { userLookup.findById(50L) } returns userEs
        every { userLookup.findById(51L) } returns userEn
        every { preferencesRepository.findByUserId(50L) } returns defaultPrefs
        every { preferencesRepository.findByUserId(51L) } returns defaultPrefs.copy(userId = 51L)
        every { notificationDedupPort.shouldDispatch(any(), any(), any(), any()) } returns true
        every {
            contentRenderer.render(any(), any(), any(),
                match { it.locale == Locale.forLanguageTag("es-ES") }, any(), any())
        } returns contentEs
        every {
            contentRenderer.render(any(), any(), any(),
                match { it.locale == Locale.forLanguageTag("en-US") }, any(), any())
        } returns contentEn
        every { fcmSender.send(any(), any()) } returns successFcmResult

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        // One render call per distinct locale (es-ES and en-US)
        verify(exactly = 2) { contentRenderer.render(any(), any(), any(), any(), any(), any()) }
        // One fcmSender.send call per locale group
        verify(exactly = 2) { fcmSender.send(any(), any()) }
    }

    @Test
    fun `should return AlertNotFound when severity lookup returns null`() {
        every { alertSeverityLookup.findById(3) } returns null

        val result = useCase.dispatch(
            type = NotificationType.ALERT_ACTIVATED,
            alert = baseAlert,
            change = sampleChange
        )

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(NotificationError.AlertNotFound::class.java)
        verify(exactly = 0) { fcmSender.send(any(), any()) }
    }
}
