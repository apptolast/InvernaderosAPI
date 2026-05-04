package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.Alert as AlertEntity
import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.user.User
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional

/**
 * Regression test for the bug where push notifications were dropped as
 * BELOW_MIN_SEVERITY because the saved Alert was returned by Hibernate
 * from the first-level cache without the LAZY `severity` relation
 * initialised. The adapter now flushes + detaches before re-loading via
 * `findById`, which forces the @EntityGraph("Alert.context") to apply
 * and the severity to be eagerly fetched.
 */
class AlertRepositoryAdapterTest {

    private val jpaRepository = mockk<AlertRepository>()
    private val entityManager = mockk<EntityManager>(relaxed = true)
    private val adapter = AlertRepositoryAdapter(jpaRepository, entityManager)

    @Test
    fun `save flushes detaches the cached entity then reloads via findById to force EntityGraph fetch`() {
        val now = Instant.parse("2026-05-03T22:00:00Z")
        val severityId: Short = 4
        val severityLevel: Short = 4
        val severity = AlertSeverity(
            id = severityId,
            name = "CRITICAL",
            level = severityLevel,
            color = "#FF0000",
            notifyPush = true
        )

        // Entity returned by save(): id assigned, but `severity` LAZY relation is null
        // (just like Hibernate's behaviour after a fresh persist).
        val rawSavedEntity = AlertEntity(
            id = 99L,
            code = "ALT-99",
            tenantId = 10L,
            sectorId = 20L,
            alertTypeId = null,
            severityId = severityId,
            message = "Temperatura crítica",
            description = null,
            clientName = null,
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            createdAt = now,
            updatedAt = now
        )

        // Reloaded entity: same row but with `severity` populated as if @EntityGraph applied.
        val reloadedEntity = AlertEntity(
            id = 99L,
            code = "ALT-99",
            tenantId = 10L,
            sectorId = 20L,
            alertTypeId = null,
            severityId = severityId,
            message = "Temperatura crítica",
            description = null,
            clientName = null,
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            createdAt = now,
            updatedAt = now
        ).apply { this.severity = severity }

        every { jpaRepository.save(any()) } returns rawSavedEntity
        every { jpaRepository.findById(99L) } returns Optional.of(reloadedEntity)
        justRun { entityManager.flush() }
        justRun { entityManager.detach(rawSavedEntity) }

        val domainInput = Alert(
            id = null,
            code = "ALT-99",
            tenantId = TenantId(10L),
            sectorId = SectorId(20L),
            sectorCode = null,
            alertTypeId = null,
            alertTypeName = null,
            severityId = severityId,
            severityName = null,
            severityLevel = null,
            message = "Temperatura crítica",
            description = null,
            clientName = null,
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            resolvedByUserName = null,
            createdAt = now,
            updatedAt = now
        )

        val result = adapter.save(domainInput)

        // 1. Critical: severity-derived fields are populated on the returned domain Alert.
        // Without the flush+detach, severityLevel would be null and the downstream
        // notification dispatch would drop the push as BELOW_MIN_SEVERITY.
        assertThat(result.severityLevel).isEqualTo(severityLevel)
        assertThat(result.severityName).isEqualTo("CRITICAL")

        // 2. Verify the order: save → flush → detach → findById.
        verifyOrder {
            jpaRepository.save(any())
            entityManager.flush()
            entityManager.detach(rawSavedEntity)
            jpaRepository.findById(99L)
        }
        verify(exactly = 1) { entityManager.flush() }
        verify(exactly = 1) { entityManager.detach(rawSavedEntity) }
    }

    @Test
    fun `save updates managed entity scalars without merging detached read-only associations`() {
        val now = Instant.parse("2026-05-03T22:00:00Z")
        val updatedAt = Instant.parse("2026-05-03T23:00:00Z")
        val tenant = Tenant(id = 10L, code = "TNT-10", name = "Tenant", email = "tenant@example.com")
        val sector = Sector(id = 20L, code = "SEC-20", tenantId = 10L, greenhouseId = 30L, name = "Sector")
        val resolvedByUser = User(
            id = 42L,
            code = "USR-42",
            tenantId = 10L,
            username = "resolver",
            email = "resolver@example.com",
            passwordHash = "hash",
            role = "USER",
        )
        val managedEntity = AlertEntity(
            id = 99L,
            code = "ALT-99",
            tenantId = 10L,
            sectorId = 20L,
            alertTypeId = null,
            severityId = null,
            message = "Old message",
            description = null,
            clientName = null,
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            createdAt = now,
            updatedAt = now
        ).apply {
            this.tenant = tenant
            this.sector = sector
        }
        val reloadedEntity = AlertEntity(
            id = 99L,
            code = "ALT-99",
            tenantId = 10L,
            sectorId = 20L,
            alertTypeId = null,
            severityId = null,
            message = "Resolved by API",
            description = "Updated",
            clientName = "client",
            isResolved = true,
            resolvedAt = updatedAt,
            resolvedByUserId = 42L,
            createdAt = now,
            updatedAt = updatedAt
        ).apply {
            this.tenant = tenant
            this.sector = sector
            this.resolvedByUser = resolvedByUser
        }
        val domainInput = Alert(
            id = 99L,
            code = "ALT-99",
            tenantId = TenantId(10L),
            sectorId = SectorId(20L),
            sectorCode = "SEC-20",
            alertTypeId = null,
            alertTypeName = null,
            severityId = null,
            severityName = null,
            severityLevel = null,
            message = "Resolved by API",
            description = "Updated",
            clientName = "client",
            isResolved = true,
            resolvedAt = updatedAt,
            resolvedByUserId = 42L,
            resolvedByUserName = "resolver",
            createdAt = now,
            updatedAt = updatedAt,
        )

        every { jpaRepository.findById(99L) } returnsMany listOf(Optional.of(managedEntity), Optional.of(reloadedEntity))
        justRun { entityManager.flush() }
        justRun { entityManager.detach(managedEntity) }

        val result = adapter.save(domainInput)

        assertThat(result.isResolved).isTrue()
        assertThat(result.resolvedByUserId).isEqualTo(42L)
        assertThat(result.resolvedByUserName).isEqualTo("resolver")
        assertThat(managedEntity.message).isEqualTo("Resolved by API")
        assertThat(managedEntity.resolvedByUserId).isEqualTo(42L)
        assertThat(managedEntity.tenant).isSameAs(tenant)
        assertThat(managedEntity.sector).isSameAs(sector)
        assertThat(managedEntity.resolvedByUser).isNull()
        verify(exactly = 0) { jpaRepository.save(any()) }
        verifyOrder {
            jpaRepository.findById(99L)
            entityManager.flush()
            entityManager.detach(managedEntity)
            jpaRepository.findById(99L)
        }
    }
}
