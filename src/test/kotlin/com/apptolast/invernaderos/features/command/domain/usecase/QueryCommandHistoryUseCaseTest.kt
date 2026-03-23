package com.apptolast.invernaderos.features.command.domain.usecase

import com.apptolast.invernaderos.features.command.application.usecase.QueryCommandHistoryUseCaseImpl
import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class QueryCommandHistoryUseCaseTest {

    private val persistence = mockk<DeviceCommandPersistencePort>()
    private val useCase = QueryCommandHistoryUseCaseImpl(persistence)

    private val sample = DeviceCommand(
        time = Instant.parse("2026-03-22T12:00:00Z"),
        code = "SET-00036",
        value = "22"
    )

    @Test
    fun `should get command history by code`() {
        every { persistence.findByCodeOrderByTimeDesc("SET-00036") } returns listOf(sample)

        val result = useCase.getHistory("SET-00036")

        assertThat(result).hasSize(1)
        assertThat(result[0].value).isEqualTo("22")
    }

    @Test
    fun `should get command history by code and time range`() {
        val from = Instant.parse("2026-03-22T00:00:00Z")
        val to = Instant.parse("2026-03-22T23:59:59Z")
        every { persistence.findByCodeAndTimeBetween("SET-00036", from, to) } returns listOf(sample)

        val result = useCase.getHistory("SET-00036", from, to)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `should get latest command`() {
        every { persistence.findLatestByCode("SET-00036") } returns sample

        val result = useCase.getLatest("SET-00036")

        assertThat(result).isNotNull
        assertThat(result!!.code).isEqualTo("SET-00036")
    }

    @Test
    fun `should return null when no commands exist`() {
        every { persistence.findLatestByCode("UNKNOWN") } returns null

        val result = useCase.getLatest("UNKNOWN")

        assertThat(result).isNull()
    }
}
