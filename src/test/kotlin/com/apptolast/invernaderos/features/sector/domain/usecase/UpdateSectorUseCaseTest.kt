package com.apptolast.invernaderos.features.sector.domain.usecase

import com.apptolast.invernaderos.features.sector.application.usecase.UpdateSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorCommand
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
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

class UpdateSectorUseCaseTest {

    private val repository = mockk<SectorRepositoryPort>()
    private val greenhouseExistence = mockk<GreenhouseExistencePort>()
    private val useCase = UpdateSectorUseCaseImpl(repository, greenhouseExistence)

    private val existing = Sector(
        id = SectorId(1L),
        code = "SEC-00001",
        tenantId = TenantId(10L),
        greenhouseId = GreenhouseId(20L),
        greenhouseCode = "GRH-00001",
        name = "Sector A"
    )

    @Test
    fun `should update sector name`() {
        val command = UpdateSectorCommand(id = SectorId(1L), tenantId = TenantId(10L), name = "Sector B")

        every { repository.findByIdAndTenantId(SectorId(1L), TenantId(10L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.name).isEqualTo("Sector B")
    }

    @Test
    fun `should return NotFound when sector does not exist`() {
        val command = UpdateSectorCommand(id = SectorId(999L), tenantId = TenantId(10L), name = "X")

        every { repository.findByIdAndTenantId(SectorId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SectorError.NotFound::class.java)
    }

    @Test
    fun `should return error when new greenhouse does not belong to tenant`() {
        val command = UpdateSectorCommand(
            id = SectorId(1L),
            tenantId = TenantId(10L),
            greenhouseId = GreenhouseId(99L)
        )

        every { repository.findByIdAndTenantId(SectorId(1L), TenantId(10L)) } returns existing
        every { greenhouseExistence.existsByIdAndTenantId(GreenhouseId(99L), TenantId(10L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SectorError.GreenhouseNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
