package com.apptolast.invernaderos.features.user.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.application.usecase.CreateUserUseCaseImpl
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserCommand
import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import com.apptolast.invernaderos.features.user.domain.port.output.UserCodeGenerator
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class CreateUserUseCaseTest {

    private val repository = mockk<UserRepositoryPort>()
    private val codeGenerator = mockk<UserCodeGenerator>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val useCase = CreateUserUseCaseImpl(repository, codeGenerator, passwordHasher)

    private val tenantId = TenantId(1L)
    private val now = Instant.now()

    private fun savedUser(id: Long, code: String, username: String, email: String) = User(
        id = id,
        code = code,
        tenantId = tenantId,
        username = username,
        email = email,
        role = UserRole.OPERATOR,
        isActive = true,
        lastLogin = null,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `should create user when username and email are unique`() {
        val command = CreateUserCommand(
            tenantId = tenantId,
            username = "jdoe",
            email = "jdoe@example.com",
            passwordRaw = "secret123",
            role = "OPERATOR",
            isActive = true
        )

        every { repository.existsByUsername("jdoe") } returns false
        every { repository.existsByEmail("jdoe@example.com") } returns false
        every { codeGenerator.generate() } returns "USR-00001"
        every { passwordHasher.hash("secret123") } returns "hashed"
        every { repository.save(any(), "hashed") } answers {
            savedUser(42L, "USR-00001", "jdoe", "jdoe@example.com")
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val user = (result as Either.Right).value
        assertThat(user.id).isEqualTo(42L)
        assertThat(user.code).isEqualTo("USR-00001")
        assertThat(user.username).isEqualTo("jdoe")
        assertThat(user.role).isEqualTo(UserRole.OPERATOR)
        verify(exactly = 1) { repository.save(any(), "hashed") }
    }

    @Test
    fun `should return InvalidRole when role is not recognized`() {
        val command = CreateUserCommand(
            tenantId = tenantId,
            username = "jdoe",
            email = "jdoe@example.com",
            passwordRaw = "secret123",
            role = "SUPERADMIN"
        )

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(UserError.InvalidRole::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }

    @Test
    fun `should return DuplicateUsername when username already exists`() {
        val command = CreateUserCommand(
            tenantId = tenantId,
            username = "existing",
            email = "new@example.com",
            passwordRaw = "secret123",
            role = "VIEWER"
        )

        every { repository.existsByUsername("existing") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(UserError.DuplicateUsername::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }

    @Test
    fun `should return DuplicateEmail when email already exists`() {
        val command = CreateUserCommand(
            tenantId = tenantId,
            username = "newuser",
            email = "existing@example.com",
            passwordRaw = "secret123",
            role = "ADMIN"
        )

        every { repository.existsByUsername("newuser") } returns false
        every { repository.existsByEmail("existing@example.com") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(UserError.DuplicateEmail::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }
}
