package com.apptolast.invernaderos.features.user.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserCommand
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import com.apptolast.invernaderos.features.user.domain.port.output.UserCodeGenerator
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class CreateUserUseCaseImpl(
    private val repository: UserRepositoryPort,
    private val codeGenerator: UserCodeGenerator,
    private val passwordHasher: PasswordHasher,
    private val applicationEventPublisher: ApplicationEventPublisher
) : CreateUserUseCase {

    override fun execute(command: CreateUserCommand): Either<UserError, User> {
        val role = parseRole(command.role) ?: return Either.Left(UserError.InvalidRole(command.role))

        if (repository.existsByUsername(command.username)) {
            return Either.Left(UserError.DuplicateUsername(command.username))
        }

        if (repository.existsByEmail(command.email)) {
            return Either.Left(UserError.DuplicateEmail(command.email))
        }

        val now = Instant.now()
        val passwordHash = passwordHasher.hash(command.passwordRaw)

        val user = User(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            username = command.username,
            email = command.email,
            role = role,
            isActive = command.isActive,
            lastLogin = null,
            createdAt = now,
            updatedAt = now
        )

        val saved = repository.save(user, passwordHash)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.USER_CRUD)
        )
        return Either.Right(saved)
    }

    private fun parseRole(role: String): UserRole? =
        runCatching { UserRole.valueOf(role.uppercase()) }.getOrNull()
}
