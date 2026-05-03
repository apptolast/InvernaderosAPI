package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertEpisode
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class FindAlertEpisodesUseCaseTest {

    private val historyQueryPort = mockk<AlertHistoryQueryPort>()
    private val useCase = FindAlertEpisodesUseCaseImpl(historyQueryPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    private fun buildQuery(
        page: Int = 0,
        size: Int = 50,
        from: Instant = this.from,
        to: Instant = this.now,
        onlyClosed: Boolean = true,
    ) = AlertEpisodesQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        severityIds = emptyList(),
        sectorIds = emptyList(),
        codes = emptyList(),
        onlyClosed = onlyClosed,
        page = page,
        size = size,
    )

    private fun buildEpisode(resolvedAt: Instant? = now) = AlertEpisode(
        alertId = 100L,
        alertCode = "ALT-00001",
        triggeredAt = from.plusSeconds(60),
        resolvedAt = resolvedAt,
        durationSeconds = if (resolvedAt != null) 3600L else null,
        triggerSource = AlertSignalSource.MQTT,
        resolveSource = if (resolvedAt != null) AlertSignalSource.API else null,
        triggerActor = AlertActor.Device(null),
        resolveActor = if (resolvedAt != null) AlertActor.User(42L, "user42", null) else null,
        severityId = null,
        severityName = null,
        sectorId = 20L,
        sectorCode = "SEC-001",
    )

    @Test
    fun `should return paged episodes when query is valid`() {
        val query = buildQuery()
        val expected = PagedResult(
            items = listOf(buildEpisode()),
            page = 0,
            size = 50,
            total = 1L,
            hasMore = false,
        )
        every { historyQueryPort.findEpisodes(query) } returns expected

        val result = useCase.findEpisodes(query)

        assertThat(result).isEqualTo(expected)
        verify(exactly = 1) { historyQueryPort.findEpisodes(query) }
    }

    @Test
    fun `should throw when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(1))

        assertThatThrownBy { useCase.findEpisodes(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("from must not be after to")
    }

    @Test
    fun `should accept from equal to to (same instant boundary)`() {
        val query = buildQuery(from = now, to = now)
        every { historyQueryPort.findEpisodes(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 50, total = 0L, hasMore = false
        )

        val result = useCase.findEpisodes(query)

        assertThat(result.items).isEmpty()
    }

    @Test
    fun `should throw when page is negative`() {
        val query = buildQuery(page = -1)

        assertThatThrownBy { useCase.findEpisodes(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("page must be non-negative")
    }

    @Test
    fun `should accept page zero as valid lower bound`() {
        val query = buildQuery(page = 0)
        every { historyQueryPort.findEpisodes(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 50, total = 0L, hasMore = false
        )

        val result = useCase.findEpisodes(query)

        assertThat(result.page).isEqualTo(0)
    }

    @Test
    fun `should throw when size exceeds 200`() {
        val query = buildQuery(size = 201)

        assertThatThrownBy { useCase.findEpisodes(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("size must be")
    }

    @Test
    fun `should throw when size is zero`() {
        val query = buildQuery(size = 0)

        assertThatThrownBy { useCase.findEpisodes(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should accept size of exactly 200 as valid upper bound`() {
        val query = buildQuery(size = 200)
        every { historyQueryPort.findEpisodes(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 200, total = 0L, hasMore = false
        )

        val result = useCase.findEpisodes(query)

        assertThat(result.size).isEqualTo(200)
    }

    @Test
    fun `should return empty list when port returns no episodes`() {
        val query = buildQuery()
        every { historyQueryPort.findEpisodes(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 50, total = 0L, hasMore = false
        )

        val result = useCase.findEpisodes(query)

        assertThat(result.items).isEmpty()
        assertThat(result.total).isZero()
    }

    @Test
    fun `should return episode with null resolvedAt when onlyClosed is false`() {
        val query = buildQuery(onlyClosed = false)
        val openEpisode = buildEpisode(resolvedAt = null)
        every { historyQueryPort.findEpisodes(query) } returns PagedResult(
            items = listOf(openEpisode), page = 0, size = 50, total = 1L, hasMore = false
        )

        val result = useCase.findEpisodes(query)

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().resolvedAt).isNull()
        assertThat(result.items.first().durationSeconds).isNull()
    }

    @Test
    fun `should not call port when from-after-to validation fails`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.findEpisodes(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { historyQueryPort.findEpisodes(any()) }
    }
}
