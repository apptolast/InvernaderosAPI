package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.input.UpdateUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either

class UpdateUserPreferencesUseCaseImpl(
    private val preferencesRepository: UserPreferencesRepositoryPort
) : UpdateUserPreferencesUseCase {

    override fun update(
        userId: Long,
        preferences: UserNotificationPreferences
    ): Either<NotificationError, UserNotificationPreferences> {
        val validationError = validatePreferences(preferences)
        if (validationError != null) {
            return Either.Left(validationError)
        }
        val savedPreferences = preferencesRepository.save(preferences.copy(userId = userId))
        return Either.Right(savedPreferences)
    }

    private fun validatePreferences(preferences: UserNotificationPreferences): NotificationError.InvalidPreferences? {
        if (preferences.minAlertSeverity < 1 || preferences.minAlertSeverity > 4) {
            return NotificationError.InvalidPreferences(
                "minAlertSeverity must be between 1 and 4, got ${preferences.minAlertSeverity}"
            )
        }
        val quietHoursStart = preferences.quietHours.start
        val quietHoursEnd = preferences.quietHours.end
        if ((quietHoursStart == null) != (quietHoursEnd == null)) {
            return NotificationError.InvalidPreferences(
                "quietHours.start and quietHours.end must both be set or both be null"
            )
        }
        return null
    }
}
