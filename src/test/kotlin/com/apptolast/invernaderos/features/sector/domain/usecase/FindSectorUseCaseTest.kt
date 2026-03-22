package com.apptolast.invernaderos.features.sector.domain.usecase

import com.apptolast.invernaderos.features.sector.application.usecase.FindSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FindSectorUseCaseTest {

    private val repository = mockk<SectorRepositoryPort>()
    private val useCase = FindSectorUseCaseImpl(repository)

    private val sampleSector = Sector(
        id = SectorId(1L),
        code = "SEC-00001",
        tenantId = TenantId(10L),
        greenhouseId = GreenhouseId(20L),
        greenhouseCode = "GRH-00001",
        name = "Sector A"
    )

    @Test
    fun `should find sector by id and tenant`() {
        every { repository.findByIdAndTenantId(SectorId(1L), TenantId(10L)) } returns sampleSector

        val result = useCase.findByIdAndTenantId(SectorId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.name).isEqualTo("Sector A")
    }

    @Test
    fun `should return NotFound when sector does not exist`() {
        every { repository.findByIdAndTenantId(SectorId(999L), TenantId(10L)) } returns null

        val result = useCase.findByIdAndTenantId(SectorId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SectorError.NotFound::class.java)
    }

    @Test
    fun `should find all sectors by tenant id`() {
        every { repository.findAllByTenantId(TenantId(10L)) } returns listOf(sampleSector)

        val result = useCase.findAllByTenantId(TenantId(10L))

        assertThat(result).hasSize(1)
    }

    @Test
    fun `should find all sectors by greenhouse id`() {
        every { repository.findAllByGreenhouseId(GreenhouseId(20L)) } returns listOf(sampleSector)

        val result = useCase.findAllByGreenhouseId(GreenhouseId(20L))

        assertThat(result).hasSize(1)
    }
}
