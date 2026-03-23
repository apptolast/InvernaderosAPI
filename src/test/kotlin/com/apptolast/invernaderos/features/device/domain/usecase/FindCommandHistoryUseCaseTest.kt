package com.apptolast.invernaderos.features.device.domain.usecase

import com.apptolast.invernaderos.features.device.application.usecase.FindCommandHistoryUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.device.domain.port.output.CommandHistoryRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindCommandHistoryUseCaseTest {

    private val repository = mockk<CommandHistoryRepositoryPort>()
    private val useCase = FindCommandHistoryUseCaseImpl(repository)

    private val sample = CommandHistory(
        id = 1L, code = "CMD-00001", deviceId = DeviceId(10L),
        command = "ON", value = null, source = "USER", userId = 1L,
        success = true, response = null, createdAt = Instant.now()
    )

    @Test
    fun `should find command history by device id`() {
        every { repository.findByDeviceId(DeviceId(10L)) } returns listOf(sample)
        val result = useCase.findByDeviceId(DeviceId(10L))
        assertThat(result).hasSize(1)
        assertThat(result[0].command).isEqualTo("ON")
    }

    @Test
    fun `should find recent commands by device`() {
        every { repository.findRecentByDevice(DeviceId(10L), 5) } returns listOf(sample)
        val result = useCase.findRecentByDevice(DeviceId(10L), 5)
        assertThat(result).hasSize(1)
    }
}
