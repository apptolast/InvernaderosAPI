package com.apptolast.invernaderos.features.greenhouse.domain.usecase

import com.apptolast.invernaderos.features.greenhouse.application.usecase.FindGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class FindGreenhouseUseCaseTest {

    private val repository = mockk<GreenhouseRepositoryPort>()
    private val useCase = FindGreenhouseUseCaseImpl(repository)

    private val sampleGreenhouse = Greenhouse(
        id = GreenhouseId(1L),
        code = "GRH-00001",
        tenantId = TenantId(10L),
        name = "Invernadero Norte",
        location = null,
        areaM2 = BigDecimal("1500.00"),
        timezone = "Europe/Madrid",
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `should find greenhouse by id and tenant`() {
        every { repository.findByIdAndTenantId(GreenhouseId(1L), TenantId(10L)) } returns sampleGreenhouse

        val result = useCase.findById(GreenhouseId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val greenhouse = (result as Either.Right).value
        assertThat(greenhouse.name).isEqualTo("Invernadero Norte")
    }

    @Test
    fun `should return NotFound when greenhouse does not exist`() {
        every { repository.findByIdAndTenantId(GreenhouseId(999L), TenantId(10L)) } returns null

        val result = useCase.findById(GreenhouseId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(GreenhouseError.NotFound::class.java)
    }

    @Test
    fun `should find all greenhouses by tenant id`() {
        val greenhouses = listOf(sampleGreenhouse, sampleGreenhouse.copy(id = GreenhouseId(2L), name = "Invernadero Sur"))
        every { repository.findAllByTenantId(TenantId(10L)) } returns greenhouses

        val result = useCase.findAllByTenantId(TenantId(10L))

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Invernadero Norte")
        assertThat(result[1].name).isEqualTo("Invernadero Sur")
    }

    @Test
    fun `should return empty list when tenant has no greenhouses`() {
        every { repository.findAllByTenantId(TenantId(99L)) } returns emptyList()

        val result = useCase.findAllByTenantId(TenantId(99L))

        assertThat(result).isEmpty()
    }
}
