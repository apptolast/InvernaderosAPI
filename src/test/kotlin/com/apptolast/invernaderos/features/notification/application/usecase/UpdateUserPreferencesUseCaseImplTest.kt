package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.PreferredChannel
import com.apptolast.invernaderos.features.notification.domain.model.QuietHours
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.ZoneId

class UpdateUserPreferencesUseCaseImplTest {

    private val preferencesRepository = mockk<UserPreferencesRepositoryPort>()
    private val useCase = UpdateUserPreferencesUseCaseImpl(preferencesRepository)

    private val validPrefs = UserNotificationPreferences(
        userId = 42L,
        categoryAlerts = true,
        categoryDevices = true,
        categorySubscription = false,
        minAlertSeverity = 2,
        quietHours = QuietHours(start = null, end = null, timezone = ZoneId.of("Europe/Madrid")),
        preferredChannel = PreferredChannel.PUSH
    )

    @Test
    fun `should return InvalidPreferences when minAlertSeverity is 0`() {
        val prefs = validPrefs.copy(minAlertSeverity = 0)

        val result = useCase.update(userId = 42L, preferences = prefs)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(NotificationError.InvalidPreferences::class.java)
        verify(exactly = 0) { preferencesRepository.save(any()) }
    }

    @Test
    fun `should return InvalidPreferences when minAlertSeverity is 5`() {
        val prefs = validPrefs.copy(minAlertSeverity = 5)

        val result = useCase.update(userId = 42L, preferences = prefs)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(NotificationError.InvalidPreferences::class.java)
        verify(exactly = 0) { preferencesRepository.save(any()) }
    }

    @Test
    fun `should accept valid preferences with minAlertSeverity 3`() {
        val prefs = validPrefs.copy(minAlertSeverity = 3)
        val savedPrefs = prefs.copy(userId = 42L)
        every { preferencesRepository.save(any()) } returns savedPrefs

        val result = useCase.update(userId = 42L, preferences = prefs)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val saved = (result as Either.Right).value
        assertThat(saved.minAlertSeverity).isEqualTo(3)
        verify(exactly = 1) { preferencesRepository.save(any()) }
    }

    @Test
    fun `should return InvalidPreferences when quietHours start is non-null and end is null`() {
        // Asymmetric quiet hours — start set, end missing → invalid
        val prefs = validPrefs.copy(
            quietHours = QuietHours(
                start = LocalTime.of(22, 0),
                end = null,
                timezone = ZoneId.of("Europe/Madrid")
            )
        )

        val result = useCase.update(userId = 42L, preferences = prefs)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(NotificationError.InvalidPreferences::class.java)
        assertThat(error.message).contains("quietHours")
        verify(exactly = 0) { preferencesRepository.save(any()) }
    }
}
