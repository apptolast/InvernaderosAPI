package com.apptolast.invernaderos.features.sensor.domain.usecase

import com.apptolast.invernaderos.features.sensor.application.usecase.QuerySensorReadingsUseCaseImpl
import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import com.apptolast.invernaderos.features.sensor.domain.port.output.SensorReadingQueryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class QuerySensorReadingsUseCaseTest {

    private val queryPort = mockk<SensorReadingQueryPort>()
    private val useCase = QuerySensorReadingsUseCaseImpl(queryPort)

    private val sample = SensorReading(
        time = Instant.parse("2026-03-22T12:00:00Z"),
        code = "DEV-00001",
        value = "23.5"
    )

    @Test
    fun `should get latest readings`() {
        every { queryPort.findTopNOrderByTimeDesc(10) } returns listOf(sample)

        val result = useCase.getLatestReadings(10)

        assertThat(result).hasSize(1)
        assertThat(result[0].code).isEqualTo("DEV-00001")
    }

    @Test
    fun `should get readings by code and time range`() {
        val start = Instant.parse("2026-03-22T00:00:00Z")
        val end = Instant.parse("2026-03-22T23:59:59Z")
        every { queryPort.findByCodeAndTimeBetween("DEV-00001", start, end) } returns listOf(sample)

        val result = useCase.getReadingsByCode("DEV-00001", start, end)

        assertThat(result).hasSize(1)
        assertThat(result[0].value).isEqualTo("23.5")
    }

    @Test
    fun `should get current values for all codes`() {
        every { queryPort.findLatestForAllCodes() } returns listOf(sample)

        val result = useCase.getCurrentValues()

        assertThat(result).containsKey("sensors")
        assertThat(result).containsKey("timestamp")
        @Suppress("UNCHECKED_CAST")
        val sensors = result["sensors"] as Map<String, Any?>
        assertThat(sensors).containsKey("DEV-00001")
    }
}
