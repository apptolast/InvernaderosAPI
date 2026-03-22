package com.apptolast.invernaderos.features.tenant.domain.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.application.usecase.UpdateTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantCommand
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class UpdateTenantUseCaseTest {

    private val repository = mockk<TenantRepositoryPort>()
    private val useCase = UpdateTenantUseCaseImpl(repository)

    private val existing = Tenant(
        id = TenantId(1L),
        code = "TNT-00001",
        name = "Elena Rodriguez",
        email = "elena@freshveg.com",
        phone = "+34 612 345 678",
        province = "Almería",
        country = "España",
        location = null,
        status = TenantStatus.ACTIVE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `should update tenant when changes are valid`() {
        val command = UpdateTenantCommand(
            id = TenantId(1L),
            name = "Elena R. Updated"
        )

        every { repository.findById(TenantId(1L)) } returns existing
        every { repository.existsByName("Elena R. Updated") } returns false
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val updated = (result as Either.Right).value
        assertThat(updated.name).isEqualTo("Elena R. Updated")
        assertThat(updated.email).isEqualTo("elena@freshveg.com")
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return NotFound when tenant does not exist`() {
        val command = UpdateTenantCommand(id = TenantId(999L), name = "Whatever")

        every { repository.findById(TenantId(999L)) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(TenantError.NotFound::class.java)
    }

    @Test
    fun `should return DuplicateName when new name exists`() {
        val command = UpdateTenantCommand(id = TenantId(1L), name = "Existing Name")

        every { repository.findById(TenantId(1L)) } returns existing
        every { repository.existsByName("Existing Name") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(TenantError.DuplicateName::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should return DuplicateEmail when new email exists`() {
        val command = UpdateTenantCommand(id = TenantId(1L), email = "taken@test.com")

        every { repository.findById(TenantId(1L)) } returns existing
        every { repository.existsByEmail("taken@test.com") } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(TenantError.DuplicateEmail::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should keep existing values when fields not provided`() {
        val command = UpdateTenantCommand(
            id = TenantId(1L),
            status = TenantStatus.INACTIVE
        )

        every { repository.findById(TenantId(1L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val updated = (result as Either.Right).value
        assertThat(updated.name).isEqualTo("Elena Rodriguez")
        assertThat(updated.status).isEqualTo(TenantStatus.INACTIVE)
    }
}
