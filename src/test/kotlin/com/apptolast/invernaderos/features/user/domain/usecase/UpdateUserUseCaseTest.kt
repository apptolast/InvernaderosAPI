package com.apptolast.invernaderos.features.user.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.application.usecase.UpdateUserUseCaseImpl
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserCommand
import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class UpdateUserUseCaseTest {

    private val repository = mockk<UserRepositoryPort>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val useCase = UpdateUserUseCaseImpl(repository, passwordHasher)

    private val tenantId = TenantId(1L)
    private val now = Instant.now()

    private val existingUser = User(
        id = 10L,
        code = "USR-00010",
        tenantId = tenantId,
        username = "jdoe",
        email = "jdoe@example.com",
        role = UserRole.OPERATOR,
        isActive = true,
        lastLogin = null,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `should update user when data is valid`() {
        val command = UpdateUserCommand(
            id = 10L,
            tenantId = tenantId,
            username = "jdoe_updated",
            email = null,
            passwordRaw = null,
            role = "ADMIN",
            isActive = null
        )

        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser
        every { repository.existsByUsername("jdoe_updated") } returns false
        every { repository.save(any(), null) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val user = (result as Either.Right).value
        assertThat(user.username).isEqualTo("jdoe_updated")
        assertThat(user.role).isEqualTo(UserRole.ADMIN)
        verify(exactly = 1) { repository.save(any(), null) }
    }

    @Test
    fun `should return NotFound when user does not exist`() {
        val command = UpdateUserCommand(id = 999L, tenantId = tenantId, username = "new")

        every { repository.findByIdAndTenantId(999L, tenantId) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(UserError.NotFound::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }

    @Test
    fun `should return DuplicateUsername when new username already taken`() {
        val command = UpdateUserCommand(id = 10L, tenantId = tenantId, username = "taken")

        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser
        every { repository.existsByUsername("taken") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(UserError.DuplicateUsername::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }

    @Test
    fun `should return DuplicateEmail when new email already taken`() {
        val command = UpdateUserCommand(id = 10L, tenantId = tenantId, email = "taken@example.com")

        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser
        every { repository.existsByEmail("taken@example.com") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(UserError.DuplicateEmail::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }

    @Test
    fun `should hash new password when passwordRaw provided`() {
        val command = UpdateUserCommand(
            id = 10L,
            tenantId = tenantId,
            passwordRaw = "newpassword"
        )

        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser
        every { passwordHasher.hash("newpassword") } returns "newhashed"
        every { repository.save(any(), "newhashed") } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { passwordHasher.hash("newpassword") }
        verify(exactly = 1) { repository.save(any(), "newhashed") }
    }

    @Test
    fun `should return InvalidRole when new role is not recognized`() {
        val command = UpdateUserCommand(id = 10L, tenantId = tenantId, role = "SUPERADMIN")

        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(UserError.InvalidRole::class.java)
        verify(exactly = 0) { repository.save(any(), any()) }
    }
}
