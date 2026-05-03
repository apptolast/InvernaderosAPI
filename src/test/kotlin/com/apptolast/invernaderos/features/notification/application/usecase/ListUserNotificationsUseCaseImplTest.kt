package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.NotificationStatus
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ListUserNotificationsUseCaseImplTest {

    private val notificationLogRepository = mockk<NotificationLogRepositoryPort>()
    private val useCase = ListUserNotificationsUseCaseImpl(notificationLogRepository)

    @Test
    fun `should delegate to repository with correct parameters`() {
        val entry = NotificationLogEntry(
            id = 1001L,
            tenantId = 10L,
            userId = 42L,
            deviceTokenId = 100L,
            notificationType = NotificationType.ALERT_ACTIVATED,
            payloadJson = """{"title":"Nueva alerta: ERROR"}""",
            status = NotificationStatus.SENT,
            fcmMessageId = "msg-abc",
            error = null,
            sentAt = Instant.parse("2026-01-01T10:00:00Z")
        )
        val page = UserNotificationLogPage(entries = listOf(entry), nextCursor = null)
        every { notificationLogRepository.listForUser(userId = 42L, cursor = null, limit = 20) } returns page

        val result = useCase.list(userId = 42L, cursor = null, limit = 20)

        assertThat(result.entries).hasSize(1)
        assertThat(result.entries.first().notificationType).isEqualTo(NotificationType.ALERT_ACTIVATED)
        assertThat(result.nextCursor).isNull()
        verify(exactly = 1) { notificationLogRepository.listForUser(userId = 42L, cursor = null, limit = 20) }
    }
}
