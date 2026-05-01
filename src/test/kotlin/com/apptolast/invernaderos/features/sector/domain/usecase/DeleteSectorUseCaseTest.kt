package com.apptolast.invernaderos.features.sector.domain.usecase

import com.apptolast.invernaderos.features.sector.application.usecase.DeleteSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
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

class DeleteSectorUseCaseTest {

    private val repository = mockk<SectorRepositoryPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = DeleteSectorUseCaseImpl(repository, applicationEventPublisher)

    private val existing = Sector(
        id = SectorId(1L),
        code = "SEC-00001",
        tenantId = TenantId(10L),
        greenhouseId = GreenhouseId(20L),
        greenhouseCode = "GRH-00001",
        name = "Sector A"
    )

    @Test
    fun `should delete sector when it exists`() {
        every { repository.findByIdAndTenantId(SectorId(1L), TenantId(10L)) } returns existing
        every { repository.delete(SectorId(1L), TenantId(10L)) } returns true

        val result = useCase.execute(SectorId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(SectorId(1L), TenantId(10L)) }
    }

    @Test
    fun `should return NotFound when sector does not exist`() {
        every { repository.findByIdAndTenantId(SectorId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(SectorId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SectorError.NotFound::class.java)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
