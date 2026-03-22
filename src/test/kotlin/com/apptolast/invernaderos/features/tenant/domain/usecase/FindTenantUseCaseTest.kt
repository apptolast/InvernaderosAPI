package com.apptolast.invernaderos.features.tenant.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.application.usecase.FindTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.input.TenantFilter
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindTenantUseCaseTest {

    private val repository = mockk<TenantRepositoryPort>()
    private val useCase = FindTenantUseCaseImpl(repository)

    private val sampleTenant = Tenant(
        id = TenantId(1L),
        code = "TNT-00001",
        name = "Elena Rodriguez",
        email = "elena@freshveg.com",
        phone = "+34 612 345 678",
        province = "Almería",
        country = "España",
        location = null,
        status = TenantStatus.ACTIVE,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `should find tenant by id`() {
        every { repository.findById(TenantId(1L)) } returns sampleTenant

        val result = useCase.findById(TenantId(1L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val tenant = (result as Either.Right).value
        assertThat(tenant.name).isEqualTo("Elena Rodriguez")
    }

    @Test
    fun `should return NotFound when tenant does not exist`() {
        every { repository.findById(TenantId(999L)) } returns null

        val result = useCase.findById(TenantId(999L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(TenantError.NotFound::class.java)
    }

    @Test
    fun `should find all tenants with filter`() {
        val filter = TenantFilter(province = "Almería")
        every { repository.findAll(filter) } returns listOf(sampleTenant)

        val result = useCase.findAll(filter)

        assertThat(result).hasSize(1)
        assertThat(result[0].province).isEqualTo("Almería")
    }

    @Test
    fun `should return empty list when no tenants match`() {
        val filter = TenantFilter(search = "NonExistent")
        every { repository.findAll(filter) } returns emptyList()

        val result = useCase.findAll(filter)

        assertThat(result).isEmpty()
    }
}
