package com.apptolast.invernaderos.features.sector.domain.usecase

import com.apptolast.invernaderos.features.sector.application.usecase.CreateSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorCommand
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorCodeGenerator
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateSectorUseCaseTest {

    private val repository = mockk<SectorRepositoryPort>()
    private val codeGenerator = mockk<SectorCodeGenerator>()
    private val greenhouseExistence = mockk<GreenhouseExistencePort>()
    private val useCase = CreateSectorUseCaseImpl(repository, codeGenerator, greenhouseExistence)

    @Test
    fun `should create sector when greenhouse belongs to tenant`() {
        val command = CreateSectorCommand(
            tenantId = TenantId(1L),
            greenhouseId = GreenhouseId(10L),
            name = "Sector A"
        )

        every { greenhouseExistence.existsByIdAndTenantId(GreenhouseId(10L), TenantId(1L)) } returns true
        every { codeGenerator.generate() } returns "SEC-00001"
        every { repository.save(any()) } answers {
            val s = firstArg<Sector>()
            s.copy(id = SectorId(100L), greenhouseCode = "GRH-00001")
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val sector = (result as Either.Right).value
        assertThat(sector.id).isEqualTo(SectorId(100L))
        assertThat(sector.code).isEqualTo("SEC-00001")
        assertThat(sector.name).isEqualTo("Sector A")
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return error when greenhouse does not belong to tenant`() {
        val command = CreateSectorCommand(
            tenantId = TenantId(1L),
            greenhouseId = GreenhouseId(99L),
            name = "Sector B"
        )

        every { greenhouseExistence.existsByIdAndTenantId(GreenhouseId(99L), TenantId(1L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(SectorError.GreenhouseNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
