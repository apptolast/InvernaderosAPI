package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ActorStatsRole
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery
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

class AlertByActorStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertByActorStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    private fun buildQuery(
        from: Instant = this.from,
        to: Instant = this.now,
        role: ActorStatsRole = ActorStatsRole.RESOLVER,
    ) = ByActorStatsQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        role = role,
    )

    @Test
    fun `should return port result unchanged when query is valid`() {
        val query = buildQuery()
        val expected = listOf(
            ByActorBucket(actorUserId = 42L, username = "user42", displayName = "User 42", count = 5L)
        )
        every { statsPort.byActor(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate role RESOLVER to the port`() {
        val querySlot = slot<ByActorStatsQuery>()
        val query = buildQuery(role = ActorStatsRole.RESOLVER)
        every { statsPort.byActor(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.role).isEqualTo(ActorStatsRole.RESOLVER)
        assertThat(querySlot.captured.tenantId).isEqualTo(tenantId)
    }

    @Test
    fun `should propagate role OPENER to the port`() {
        val querySlot = slot<ByActorStatsQuery>()
        val query = buildQuery(role = ActorStatsRole.OPENER)
        every { statsPort.byActor(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.role).isEqualTo(ActorStatsRole.OPENER)
    }

    @Test
    fun `should throw when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(1))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("from must not be after to")
    }

    @Test
    fun `should return empty list when port returns empty`() {
        val query = buildQuery()
        every { statsPort.byActor(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not call port when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.byActor(any()) }
    }

    @Test
    fun `should return multiple actors ordered by count`() {
        val query = buildQuery()
        val expected = listOf(
            ByActorBucket(actorUserId = 42L, username = "user42", displayName = "User 42", count = 10L),
            ByActorBucket(actorUserId = 7L, username = "user7", displayName = "User 7", count = 3L),
        )
        every { statsPort.byActor(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).hasSize(2)
        assertThat(result[0].count).isGreaterThan(result[1].count)
    }
}
