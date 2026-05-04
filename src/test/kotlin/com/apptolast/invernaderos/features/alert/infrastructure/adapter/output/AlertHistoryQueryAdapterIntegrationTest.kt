package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.TransitionKind
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * SQL contract tests against the live DEV `metadata` Postgres.
 *
 * Goal: prove that every native SQL the adapter emits PARSES and EXECUTES
 * against a real Postgres 16, exercising columns and aliases that JdbcTemplate
 * unit-stubs would never catch. PR-1 (asc reserved-word alias + non-existent
 * `users.display_name`) shipped to prod precisely because there was no test
 * at this layer.
 *
 * We do NOT assert on row content — DEV DB is shared and rebuilt by Flyway,
 * so seeding fixtures here would race with other tests. The contract these
 * tests defend is "SQL is valid"; row content is covered by smoke curl
 * post-deploy and unit tests on the use-case layer.
 *
 * Requires INVERNADEROS_TEST_DB_PASSWORD env var to be set (resolved in
 * src/test/resources/application.yaml).
 */
@SpringBootTest
class AlertHistoryQueryAdapterIntegrationTest {

    @Autowired
    private lateinit var adapter: AlertHistoryQueryAdapter

    private val anyTenant = TenantId(1L)
    private val anyAlertId = 1L

    private fun defaultEventsQuery(): AlertEventsQuery = AlertEventsQuery(
        tenantId = anyTenant,
        from = Instant.now().minus(30, ChronoUnit.DAYS),
        to = Instant.now(),
        sources = emptyList(),
        severityIds = emptyList(),
        alertTypeIds = emptyList(),
        sectorIds = emptyList(),
        greenhouseIds = emptyList(),
        codes = emptyList(),
        actorUserIds = emptyList(),
        transitionKind = TransitionKind.ANY,
        page = 0,
        size = 50,
    )

    private fun defaultEpisodesQuery(): AlertEpisodesQuery = AlertEpisodesQuery(
        tenantId = anyTenant,
        from = Instant.now().minus(30, ChronoUnit.DAYS),
        to = Instant.now(),
        severityIds = emptyList(),
        sectorIds = emptyList(),
        codes = emptyList(),
        onlyClosed = false,
        page = 0,
        size = 50,
    )

    @Test
    fun `findTransitionsByAlertId DESC parses and executes`() {
        val rows = adapter.findTransitionsByAlertId(anyAlertId, anyTenant, SortOrder.DESC)
        assertThat(rows).isNotNull
    }

    @Test
    fun `findTransitionsByAlertId ASC parses and executes`() {
        val rows = adapter.findTransitionsByAlertId(anyAlertId, anyTenant, SortOrder.ASC)
        assertThat(rows).isNotNull
    }

    @Test
    fun `findTransitions with no filters parses and executes`() {
        val result = adapter.findTransitions(defaultEventsQuery())
        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(50)
    }

    @Test
    fun `findTransitions with all filters parses and executes`() {
        val q = defaultEventsQuery().copy(
            sources = listOf("MQTT", "API", "SYSTEM"),
            severityIds = listOf(1, 2, 3, 4),
            alertTypeIds = listOf(1, 2),
            sectorIds = listOf(1L, 2L),
            greenhouseIds = listOf(1L),
            codes = listOf("ALT-00001"),
            actorUserIds = listOf(1L),
            transitionKind = TransitionKind.OPEN,
        )
        val result = adapter.findTransitions(q)
        assertThat(result).isNotNull
    }

    @Test
    fun `findTransitions with transitionKind CLOSE parses and executes`() {
        val q = defaultEventsQuery().copy(transitionKind = TransitionKind.CLOSE)
        val result = adapter.findTransitions(q)
        assertThat(result).isNotNull
    }

    @Test
    fun `findEpisodes with no filters parses and executes`() {
        val result = adapter.findEpisodes(defaultEpisodesQuery())
        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(50)
    }

    @Test
    fun `findEpisodes with onlyClosed=true parses and executes`() {
        val q = defaultEpisodesQuery().copy(onlyClosed = true)
        val result = adapter.findEpisodes(q)
        assertThat(result).isNotNull
    }

    @Test
    fun `findEpisodes with all filters parses and executes`() {
        val q = defaultEpisodesQuery().copy(
            severityIds = listOf(1, 2),
            sectorIds = listOf(1L),
            codes = listOf("ALT-00001"),
            onlyClosed = true,
        )
        val result = adapter.findEpisodes(q)
        assertThat(result).isNotNull
    }
}
