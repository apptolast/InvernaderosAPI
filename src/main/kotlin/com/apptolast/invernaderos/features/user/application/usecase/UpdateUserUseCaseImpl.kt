package com.apptolast.invernaderos.features.user.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserCommand
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class UpdateUserUseCaseImpl(
    private val repository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
    private val applicationEventPublisher: ApplicationEventPublisher
) : UpdateUserUseCase {

    override fun execute(command: UpdateUserCommand): Either<UserError, User> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(UserError.NotFound(command.id, command.tenantId))

        if (command.username != null && command.username != existing.username) {
            if (repository.existsByUsername(command.username)) {
                return Either.Left(UserError.DuplicateUsername(command.username))
            }
        }

        if (command.email != null && command.email != existing.email) {
            if (repository.existsByEmail(command.email)) {
                return Either.Left(UserError.DuplicateEmail(command.email))
            }
        }

        val newRole: UserRole? = if (command.role != null) {
            parseRole(command.role) ?: return Either.Left(UserError.InvalidRole(command.role))
        } else null

        val updatedUser = existing.copy(
            username = command.username ?: existing.username,
            email = command.email ?: existing.email,
            role = newRole ?: existing.role,
            isActive = command.isActive ?: existing.isActive,
            updatedAt = Instant.now()
        )

        val newPasswordHash = command.passwordRaw?.let { passwordHasher.hash(it) }
        val saved = repository.save(updatedUser, newPasswordHash)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.USER_CRUD)
        )
        return Either.Right(saved)
    }

    private fun parseRole(role: String): UserRole? =
        runCatching { UserRole.valueOf(role.uppercase()) }.getOrNull()
}
