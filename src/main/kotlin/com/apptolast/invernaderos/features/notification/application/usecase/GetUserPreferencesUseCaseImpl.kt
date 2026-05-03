package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.input.GetUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort

class GetUserPreferencesUseCaseImpl(
    private val preferencesRepository: UserPreferencesRepositoryPort
) : GetUserPreferencesUseCase {

    override fun get(userId: Long): UserNotificationPreferences {
        val existingPreferences = preferencesRepository.findByUserId(userId)
        if (existingPreferences != null) {
            return existingPreferences
        }
        val defaultPreferences = UserNotificationPreferences.default(userId)
        return preferencesRepository.save(defaultPreferences)
    }
}
