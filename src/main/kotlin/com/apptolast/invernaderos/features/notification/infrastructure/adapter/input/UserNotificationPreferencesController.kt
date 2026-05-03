package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.domain.port.input.GetUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.UpdateUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.dto.mapper.toDomain
import com.apptolast.invernaderos.features.notification.dto.mapper.toResponse
import com.apptolast.invernaderos.features.notification.dto.request.UpdateUserNotificationPreferencesRequest
import com.apptolast.invernaderos.features.notification.dto.response.UserNotificationPreferencesResponse
import com.apptolast.invernaderos.features.user.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@CrossOrigin(origins = ["*"])
@Tag(
    name = "User Notification Preferences",
    description = "Per-user notification preferences (categories, severity, quiet hours, locale)"
)
@SecurityRequirement(name = "bearerAuth")
class UserNotificationPreferencesController(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "Get notification preferences for the authenticated user")
    fun get(authentication: Authentication): ResponseEntity<UserNotificationPreferencesResponse> {
        logger.debug("GET /api/v1/users/me/notification-preferences user={}", authentication.name)
        val user = resolveUser(authentication.name)
        val userId = user.id ?: throw IllegalStateException("User has null id: ${user.email}")
        val preferences = getUserPreferencesUseCase.get(userId)
        return ResponseEntity.ok(preferences.toResponse(locale = user.locale))
    }

    @PutMapping
    @Operation(summary = "Update notification preferences for the authenticated user")
    fun update(
        @Valid @RequestBody request: UpdateUserNotificationPreferencesRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        logger.debug("PUT /api/v1/users/me/notification-preferences user={}", authentication.name)
        val user = resolveUser(authentication.name)
        val userId = user.id ?: throw IllegalStateException("User has null id: ${user.email}")

        val preferences = request.toDomain(userId)
        val result = updateUserPreferencesUseCase.update(userId, preferences)

        return result.fold(
            onLeft = { error ->
                logger.warn("Failed to update preferences for user={}: {}", userId, error.message)
                ResponseEntity.badRequest().body(mapOf("error" to error.message))
            },
            onRight = { saved ->
                if (user.locale != request.locale) {
                    user.locale = request.locale
                    userRepository.save(user)
                    logger.info("Updated locale for user={} to {}", userId, request.locale)
                }
                logger.info("Updated notification preferences for user={}", userId)
                ResponseEntity.ok(saved.toResponse(locale = request.locale))
            }
        )
    }

    private fun resolveUser(principal: String) =
        userRepository.findByEmail(principal)
            ?: userRepository.findByUsername(principal)
            ?: throw UsernameNotFoundException("Authenticated user not found in DB: $principal")
}
