package com.apptolast.invernaderos.features.user.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.application.usecase.DeleteUserUseCaseImpl
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class DeleteUserUseCaseTest {

    private val repository = mockk<UserRepositoryPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = DeleteUserUseCaseImpl(repository, applicationEventPublisher)

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
    fun `should delete user when it exists`() {
        every { repository.findByIdAndTenantId(10L, tenantId) } returns existingUser
        every { repository.delete(10L, tenantId) } returns true

        val result = useCase.execute(10L, tenantId)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(10L, tenantId) }
    }

    @Test
    fun `should return NotFound when user does not exist`() {
        every { repository.findByIdAndTenantId(999L, tenantId) } returns null

        val result = useCase.execute(999L, tenantId)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        assertThat((error as UserError.NotFound).id).isEqualTo(999L)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
