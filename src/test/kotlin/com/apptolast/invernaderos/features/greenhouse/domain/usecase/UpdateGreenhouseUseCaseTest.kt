package com.apptolast.invernaderos.features.greenhouse.domain.usecase

import com.apptolast.invernaderos.features.greenhouse.application.usecase.UpdateGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class UpdateGreenhouseUseCaseTest {

    private val repository = mockk<GreenhouseRepositoryPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = UpdateGreenhouseUseCaseImpl(repository, applicationEventPublisher)

    private val existing = Greenhouse(
        id = GreenhouseId(1L),
        code = "GRH-00001",
        tenantId = TenantId(10L),
        name = "Invernadero Norte",
        location = null,
        areaM2 = BigDecimal("1500.00"),
        timezone = "Europe/Madrid",
        isActive = true,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `should update greenhouse name when new name is unique`() {
        val command = UpdateGreenhouseCommand(
            id = GreenhouseId(1L),
            tenantId = TenantId(10L),
            name = "Invernadero Sur"
        )

        every { repository.findByIdAndTenantId(GreenhouseId(1L), TenantId(10L)) } returns existing
        every { repository.existsByNameAndTenantId("Invernadero Sur", TenantId(10L)) } returns false
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val updated = (result as Either.Right).value
        assertThat(updated.name).isEqualTo("Invernadero Sur")
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return NotFound when greenhouse does not exist`() {
        val command = UpdateGreenhouseCommand(
            id = GreenhouseId(999L),
            tenantId = TenantId(10L),
            name = "Whatever"
        )

        every { repository.findByIdAndTenantId(GreenhouseId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(GreenhouseError.NotFound::class.java)
    }

    @Test
    fun `should return DuplicateName when new name already exists`() {
        val command = UpdateGreenhouseCommand(
            id = GreenhouseId(1L),
            tenantId = TenantId(10L),
            name = "Existing Name"
        )

        every { repository.findByIdAndTenantId(GreenhouseId(1L), TenantId(10L)) } returns existing
        every { repository.existsByNameAndTenantId("Existing Name", TenantId(10L)) } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(GreenhouseError.DuplicateName::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should keep existing name when name is not provided`() {
        val command = UpdateGreenhouseCommand(
            id = GreenhouseId(1L),
            tenantId = TenantId(10L),
            isActive = false
        )

        every { repository.findByIdAndTenantId(GreenhouseId(1L), TenantId(10L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val updated = (result as Either.Right).value
        assertThat(updated.name).isEqualTo("Invernadero Norte")
        assertThat(updated.isActive).isFalse()
    }
}
