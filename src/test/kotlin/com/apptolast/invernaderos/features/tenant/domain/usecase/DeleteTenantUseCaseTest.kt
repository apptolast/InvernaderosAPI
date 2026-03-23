package com.apptolast.invernaderos.features.tenant.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.application.usecase.DeleteTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class DeleteTenantUseCaseTest {

    private val repository = mockk<TenantRepositoryPort>()
    private val useCase = DeleteTenantUseCaseImpl(repository)

    private val existing = Tenant(
        id = TenantId(1L),
        code = "TNT-00001",
        name = "Elena Rodriguez",
        email = "elena@freshveg.com",
        phone = null,
        province = null,
        country = "España",
        location = null,
        status = TenantStatus.ACTIVE,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `should delete tenant when it exists`() {
        every { repository.findById(TenantId(1L)) } returns existing
        every { repository.delete(TenantId(1L)) } returns true

        val result = useCase.execute(TenantId(1L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(TenantId(1L)) }
    }

    @Test
    fun `should return NotFound when tenant does not exist`() {
        every { repository.findById(TenantId(999L)) } returns null

        val result = useCase.execute(TenantId(999L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(TenantError.NotFound::class.java)
        verify(exactly = 0) { repository.delete(any()) }
    }
}
