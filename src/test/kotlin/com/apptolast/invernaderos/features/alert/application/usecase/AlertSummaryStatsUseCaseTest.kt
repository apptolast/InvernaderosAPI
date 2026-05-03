package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class AlertSummaryStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertSummaryStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    @Test
    fun `should return port result unchanged when parameters are valid`() {
        val expected = AlertStatsSummary(
            totalActiveNow = 3L,
            openedToday = 7L,
            closedToday = 4L,
            mttrTodaySeconds = 1234.5,
            top3RecurrentCodesThisWeek = listOf("ALT-001", "ALT-002", "ALT-003"),
        )
        every { statsPort.summary(tenantId, from, now) } returns expected

        val result = useCase.execute(tenantId, from, now)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate tenantId to the port unchanged`() {
        val tenantSlot = slot<TenantId>()
        val fromSlot = slot<Instant>()
        val toSlot = slot<Instant>()
        every { statsPort.summary(capture(tenantSlot), capture(fromSlot), capture(toSlot)) } returns AlertStatsSummary(
            totalActiveNow = 0L, openedToday = 0L, closedToday = 0L,
            mttrTodaySeconds = null, top3RecurrentCodesThisWeek = emptyList()
        )

        useCase.execute(TenantId(999L), from, now)

        assertThat(tenantSlot.captured.value).isEqualTo(999L)
        assertThat(fromSlot.captured).isEqualTo(from)
        assertThat(toSlot.captured).isEqualTo(now)
    }

    @Test
    fun `should throw when from is after to`() {
        assertThatThrownBy { useCase.execute(tenantId, now, now.minusSeconds(1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("from must not be after to")
    }

    @Test
    fun `should not call port when from is after to`() {
        assertThatThrownBy { useCase.execute(tenantId, now, now.minusSeconds(3600)) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.summary(any(), any(), any()) }
    }

    @Test
    fun `should handle null mttrTodaySeconds in summary`() {
        val expected = AlertStatsSummary(
            totalActiveNow = 0L,
            openedToday = 0L,
            closedToday = 0L,
            mttrTodaySeconds = null,
            top3RecurrentCodesThisWeek = emptyList(),
        )
        every { statsPort.summary(tenantId, from, now) } returns expected

        val result = useCase.execute(tenantId, from, now)

        assertThat(result.mttrTodaySeconds).isNull()
    }

    @Test
    fun `should handle empty top3 recurrent codes`() {
        val expected = AlertStatsSummary(
            totalActiveNow = 1L,
            openedToday = 1L,
            closedToday = 0L,
            mttrTodaySeconds = null,
            top3RecurrentCodesThisWeek = emptyList(),
        )
        every { statsPort.summary(tenantId, from, now) } returns expected

        val result = useCase.execute(tenantId, from, now)

        assertThat(result.top3RecurrentCodesThisWeek).isEmpty()
    }

    @Test
    fun `should accept from equal to to (point-in-time query)`() {
        val expectedSummary = AlertStatsSummary(
            totalActiveNow = 2L,
            openedToday = 0L,
            closedToday = 0L,
            mttrTodaySeconds = null,
            top3RecurrentCodesThisWeek = emptyList(),
        )
        every { statsPort.summary(tenantId, now, now) } returns expectedSummary

        val result = useCase.execute(tenantId, now, now)

        assertThat(result.totalActiveNow).isEqualTo(2L)
    }
}
