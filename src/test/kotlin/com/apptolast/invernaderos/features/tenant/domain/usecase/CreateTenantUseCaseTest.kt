package com.apptolast.invernaderos.features.tenant.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.application.usecase.CreateTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantCommand
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantCodeGenerator
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateTenantUseCaseTest {

    private val repository = mockk<TenantRepositoryPort>()
    private val codeGenerator = mockk<TenantCodeGenerator>()
    private val useCase = CreateTenantUseCaseImpl(repository, codeGenerator)

    @Test
    fun `should create tenant when name and email are unique`() {
        val command = CreateTenantCommand(
            name = "Elena Rodriguez",
            email = "elena@freshveg.com",
            phone = "+34 612 345 678",
            province = "Almería",
            country = "España",
            location = null,
            status = TenantStatus.ACTIVE
        )

        every { repository.existsByName("Elena Rodriguez") } returns false
        every { repository.existsByEmail("elena@freshveg.com") } returns false
        every { codeGenerator.generate() } returns "TNT-00001"
        every { repository.save(any()) } answers {
            val t = firstArg<Tenant>()
            t.copy(id = TenantId(100L))
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val tenant = (result as Either.Right).value
        assertThat(tenant.id).isEqualTo(TenantId(100L))
        assertThat(tenant.code).isEqualTo("TNT-00001")
        assertThat(tenant.name).isEqualTo("Elena Rodriguez")
        assertThat(tenant.status).isEqualTo(TenantStatus.ACTIVE)
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return DuplicateName error when name exists`() {
        val command = CreateTenantCommand(
            name = "Existing",
            email = "new@test.com",
            status = TenantStatus.ACTIVE
        )

        every { repository.existsByName("Existing") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(TenantError.DuplicateName::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should return DuplicateEmail error when email exists`() {
        val command = CreateTenantCommand(
            name = "New Tenant",
            email = "existing@test.com",
            status = TenantStatus.ACTIVE
        )

        every { repository.existsByName("New Tenant") } returns false
        every { repository.existsByEmail("existing@test.com") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(TenantError.DuplicateEmail::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
