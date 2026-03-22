package com.apptolast.invernaderos.features.user.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.application.usecase.FindUserUseCaseImpl
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindUserUseCaseTest {

    private val repository = mockk<UserRepositoryPort>()
    private val useCase = FindUserUseCaseImpl(repository)

    private val tenantId = TenantId(1L)
    private val now = Instant.now()

    private fun aUser(id: Long) = User(
        id = id,
        code = "USR-0000$id",
        tenantId = tenantId,
        username = "user$id",
        email = "user$id@example.com",
        role = UserRole.VIEWER,
        isActive = true,
        lastLogin = null,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `should return user when found by id and tenantId`() {
        every { repository.findByIdAndTenantId(10L, tenantId) } returns aUser(10L)

        val result = useCase.findByIdAndTenantId(10L, tenantId)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val user = (result as Either.Right).value
        assertThat(user.id).isEqualTo(10L)
        assertThat(user.username).isEqualTo("user10")
    }

    @Test
    fun `should return NotFound when user does not exist`() {
        every { repository.findByIdAndTenantId(999L, tenantId) } returns null

        val result = useCase.findByIdAndTenantId(999L, tenantId)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(UserError.NotFound::class.java)
        assertThat((error as UserError.NotFound).id).isEqualTo(999L)
    }

    @Test
    fun `should return all users for a tenant`() {
        every { repository.findAllByTenantId(tenantId) } returns listOf(aUser(1L), aUser(2L))

        val result = useCase.findAllByTenantId(tenantId)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `should return empty list when tenant has no users`() {
        every { repository.findAllByTenantId(tenantId) } returns emptyList()

        val result = useCase.findAllByTenantId(tenantId)

        assertThat(result).isEmpty()
    }
}
