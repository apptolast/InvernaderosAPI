package com.apptolast.invernaderos.features.greenhouse.domain.usecase

import com.apptolast.invernaderos.features.greenhouse.application.usecase.DeleteGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
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

class DeleteGreenhouseUseCaseTest {

    private val repository = mockk<GreenhouseRepositoryPort>()
    private val useCase = DeleteGreenhouseUseCaseImpl(repository)

    private val existing = Greenhouse(
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
    fun `should delete greenhouse when it exists`() {
        every { repository.findByIdAndTenantId(GreenhouseId(1L), TenantId(10L)) } returns existing
        every { repository.delete(GreenhouseId(1L), TenantId(10L)) } returns true

        val result = useCase.execute(GreenhouseId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(GreenhouseId(1L), TenantId(10L)) }
    }

    @Test
    fun `should return NotFound when greenhouse does not exist`() {
        every { repository.findByIdAndTenantId(GreenhouseId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(GreenhouseId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(GreenhouseError.NotFound::class.java)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
