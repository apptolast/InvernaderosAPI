package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalDecision
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties.ValueTrueMeans.ACTIVE
import com.apptolast.invernaderos.features.alert.infrastructure.config.AlertMqttProperties.ValueTrueMeans.RESOLVED
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AlertSignalDecisionAdapterTest {

    // ---------------------------------------------------------------------------
    // Fixtures — constructed directly, no Spring context needed
    // ---------------------------------------------------------------------------

    private val adapterActive = AlertSignalDecisionAdapter(AlertMqttProperties(valueTrueMeans = ACTIVE))
    private val adapterResolved = AlertSignalDecisionAdapter(AlertMqttProperties(valueTrueMeans = RESOLVED))

    private fun anAlert(isResolved: Boolean) = Alert(
        id = 1L,
        code = "ALT-00010",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        message = "Test alert",
        description = null,
        clientName = null,
        isResolved = isResolved,
        resolvedAt = if (isResolved) Instant.parse("2026-01-02T00:00:00Z") else null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private fun signal(rawValue: String) = AlertMqttSignal(code = "ALT-00010", rawValue = rawValue)

    // ---------------------------------------------------------------------------
    // valueTrueMeans = ACTIVE (default)
    // ---------------------------------------------------------------------------

    @Test
    fun `ACTIVE config - active alert receives true - returns NO_OP`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal("true"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `ACTIVE config - active alert receives 1 - returns NO_OP`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal("1"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `ACTIVE config - active alert receives false - returns RESOLVE`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal("false"))
        assertThat(decision).isEqualTo(AlertSignalDecision.RESOLVE)
    }

    @Test
    fun `ACTIVE config - active alert receives 0 - returns RESOLVE`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal("0"))
        assertThat(decision).isEqualTo(AlertSignalDecision.RESOLVE)
    }

    @Test
    fun `ACTIVE config - resolved alert receives true - returns ACTIVATE`() {
        val decision = adapterActive.decide(anAlert(isResolved = true), signal("true"))
        assertThat(decision).isEqualTo(AlertSignalDecision.ACTIVATE)
    }

    @Test
    fun `ACTIVE config - resolved alert receives 1 - returns ACTIVATE`() {
        val decision = adapterActive.decide(anAlert(isResolved = true), signal("1"))
        assertThat(decision).isEqualTo(AlertSignalDecision.ACTIVATE)
    }

    @Test
    fun `ACTIVE config - resolved alert receives false - returns NO_OP`() {
        val decision = adapterActive.decide(anAlert(isResolved = true), signal("false"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `ACTIVE config - resolved alert receives 0 - returns NO_OP`() {
        val decision = adapterActive.decide(anAlert(isResolved = true), signal("0"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    // ---------------------------------------------------------------------------
    // valueTrueMeans = RESOLVED (inverse mapping)
    // ---------------------------------------------------------------------------

    @Test
    fun `RESOLVED config - active alert receives true - returns RESOLVE`() {
        val decision = adapterResolved.decide(anAlert(isResolved = false), signal("true"))
        assertThat(decision).isEqualTo(AlertSignalDecision.RESOLVE)
    }

    @Test
    fun `RESOLVED config - active alert receives 1 - returns RESOLVE`() {
        val decision = adapterResolved.decide(anAlert(isResolved = false), signal("1"))
        assertThat(decision).isEqualTo(AlertSignalDecision.RESOLVE)
    }

    @Test
    fun `RESOLVED config - active alert receives false - returns NO_OP`() {
        val decision = adapterResolved.decide(anAlert(isResolved = false), signal("false"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `RESOLVED config - active alert receives 0 - returns NO_OP`() {
        val decision = adapterResolved.decide(anAlert(isResolved = false), signal("0"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `RESOLVED config - resolved alert receives true - returns NO_OP`() {
        val decision = adapterResolved.decide(anAlert(isResolved = true), signal("true"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `RESOLVED config - resolved alert receives 1 - returns NO_OP`() {
        val decision = adapterResolved.decide(anAlert(isResolved = true), signal("1"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `RESOLVED config - resolved alert receives false - returns ACTIVATE`() {
        val decision = adapterResolved.decide(anAlert(isResolved = true), signal("false"))
        assertThat(decision).isEqualTo(AlertSignalDecision.ACTIVATE)
    }

    @Test
    fun `RESOLVED config - resolved alert receives 0 - returns ACTIVATE`() {
        val decision = adapterResolved.decide(anAlert(isResolved = true), signal("0"))
        assertThat(decision).isEqualTo(AlertSignalDecision.ACTIVATE)
    }

    // ---------------------------------------------------------------------------
    // Unparseable raw values — defensive fallback returns NO_OP
    // ---------------------------------------------------------------------------

    @Test
    fun `unparseable rawValue returns NO_OP for ACTIVE config`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal("banana"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `unparseable rawValue returns NO_OP for RESOLVED config`() {
        val decision = adapterResolved.decide(anAlert(isResolved = true), signal("maybe"))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }

    @Test
    fun `empty string rawValue returns NO_OP`() {
        val decision = adapterActive.decide(anAlert(isResolved = false), signal(""))
        assertThat(decision).isEqualTo(AlertSignalDecision.NO_OP)
    }
}
