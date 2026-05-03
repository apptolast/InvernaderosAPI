package com.apptolast.invernaderos.features.notification.infrastructure.adapter.input

import com.apptolast.invernaderos.features.notification.domain.port.input.ListUserNotificationsUseCase
import com.apptolast.invernaderos.features.notification.dto.mapper.toResponse
import com.apptolast.invernaderos.features.notification.dto.response.UserNotificationLogPageResponse
import com.apptolast.invernaderos.features.user.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notifications")
@CrossOrigin(origins = ["*"])
@Tag(
    name = "User Notification Log",
    description = "Paginated history of push notifications received"
)
@SecurityRequirement(name = "bearerAuth")
@Validated
class UserNotificationLogController(
    private val listUserNotificationsUseCase: ListUserNotificationsUseCase,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "List push notifications received by the authenticated user (cursor-based pagination)")
    fun list(
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) limit: Int,
        authentication: Authentication
    ): ResponseEntity<UserNotificationLogPageResponse> {
        logger.debug(
            "GET /api/v1/users/me/notifications user={} cursor={} limit={}",
            authentication.name, cursor, limit
        )
        val user = resolveUser(authentication.name)
        val userId = user.id ?: throw IllegalStateException("User has null id: ${user.email}")

        val page = listUserNotificationsUseCase.list(userId, cursor, limit)
        return ResponseEntity.ok(page.toResponse(objectMapper))
    }

    private fun resolveUser(principal: String) =
        userRepository.findByEmail(principal)
            ?: userRepository.findByUsername(principal)
            ?: throw UsernameNotFoundException("Authenticated user not found in DB: $principal")
}
