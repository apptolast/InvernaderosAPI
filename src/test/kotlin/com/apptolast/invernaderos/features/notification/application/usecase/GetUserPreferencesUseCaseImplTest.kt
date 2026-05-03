package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GetUserPreferencesUseCaseImplTest {

    private val preferencesRepository = mockk<UserPreferencesRepositoryPort>()
    private val useCase = GetUserPreferencesUseCaseImpl(preferencesRepository)

    @Test
    fun `should return existing preferences when found`() {
        val existingPrefs = UserNotificationPreferences.default(userId = 7L).copy(minAlertSeverity = 3)
        every { preferencesRepository.findByUserId(7L) } returns existingPrefs

        val result = useCase.get(userId = 7L)

        assertThat(result.minAlertSeverity).isEqualTo(3)
        assertThat(result.userId).isEqualTo(7L)
        // Repository must NOT call save when preferences already exist
        verify(exactly = 0) { preferencesRepository.save(any()) }
    }

    @Test
    fun `should return defaults via UserNotificationPreferences default when not found`() {
        val defaults = UserNotificationPreferences.default(userId = 8L)
        every { preferencesRepository.findByUserId(8L) } returns null
        every { preferencesRepository.save(any()) } returns defaults

        val result = useCase.get(userId = 8L)

        assertThat(result.categoryAlerts).isTrue()
        assertThat(result.categoryDevices).isTrue()
        assertThat(result.minAlertSeverity).isEqualTo(1)
        assertThat(result.userId).isEqualTo(8L)
        verify(exactly = 1) { preferencesRepository.save(any()) }
    }
}
