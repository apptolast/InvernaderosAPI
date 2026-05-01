package com.apptolast.invernaderos.features.greenhouse.domain.usecase

import com.apptolast.invernaderos.features.greenhouse.application.usecase.CreateGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseCodeGenerator
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CreateGreenhouseUseCaseTest {

    private val repository = mockk<GreenhouseRepositoryPort>()
    private val codeGenerator = mockk<GreenhouseCodeGenerator>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = CreateGreenhouseUseCaseImpl(repository, codeGenerator, applicationEventPublisher)

    @Test
    fun `should create greenhouse when name is unique`() {
        val tenantId = TenantId(1L)
        val command = CreateGreenhouseCommand(
            tenantId = tenantId,
            name = "Invernadero Norte",
            location = null,
            areaM2 = BigDecimal("1500.00"),
            timezone = "Europe/Madrid",
            isActive = true
        )

        every { repository.existsByNameAndTenantId("Invernadero Norte", tenantId) } returns false
        every { codeGenerator.generate() } returns "GRH-00001"
        every { repository.save(any()) } answers {
            val gh = firstArg<Greenhouse>()
            gh.copy(id = GreenhouseId(100L))
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val greenhouse = (result as Either.Right).value
        assertThat(greenhouse.id).isEqualTo(GreenhouseId(100L))
        assertThat(greenhouse.code).isEqualTo("GRH-00001")
        assertThat(greenhouse.name).isEqualTo("Invernadero Norte")
        assertThat(greenhouse.tenantId).isEqualTo(tenantId)
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return DuplicateName error when name already exists`() {
        val tenantId = TenantId(1L)
        val command = CreateGreenhouseCommand(
            tenantId = tenantId,
            name = "Existing",
            location = null,
            areaM2 = null,
            timezone = "Europe/Madrid",
            isActive = true
        )

        every { repository.existsByNameAndTenantId("Existing", tenantId) } returns true

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(GreenhouseError.DuplicateName::class.java)
        assertThat((error as GreenhouseError.DuplicateName).name).isEqualTo("Existing")
        verify(exactly = 0) { repository.save(any()) }
    }
}
