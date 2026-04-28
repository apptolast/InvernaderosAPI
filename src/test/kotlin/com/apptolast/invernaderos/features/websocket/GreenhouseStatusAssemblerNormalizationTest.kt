package com.apptolast.invernaderos.features.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Pure tests for the BOOLEAN currentValue normalisation in GreenhouseStatusAssembler.
 *
 * Pins down the contract between the API and any UI consumer of the WebSocket status
 * payload: BOOLEAN-typed codes always travel as the canonical strings "true" / "false",
 * never as "1" / "0", regardless of how the value was stored in TimescaleDB.
 *
 * For non-boolean dataTypes the value is passed through unchanged so DOUBLE / INTEGER /
 * STRING readings keep their wire representation.
 */
class GreenhouseStatusAssemblerNormalizationTest {

    @Test
    fun `BOOLEAN dataType maps stored 1 to true`() {
        val out = GreenhouseStatusAssembler.normalizeCurrentValue("1", "BOOLEAN")
        assertThat(out).isEqualTo("true")
    }

    @Test
    fun `BOOLEAN dataType maps stored 0 to false`() {
        val out = GreenhouseStatusAssembler.normalizeCurrentValue("0", "BOOLEAN")
        assertThat(out).isEqualTo("false")
    }

    @Test
    fun `BOOLEAN dataType is case-insensitive on the dataType name`() {
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", "boolean")).isEqualTo("true")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("0", "Boolean")).isEqualTo("false")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", "BoOlEaN")).isEqualTo("true")
    }

    @Test
    fun `BOOLEAN dataType passes already-canonical true and false unchanged`() {
        // If a future caller pre-normalises (or the source already had "true"/"false"),
        // we don't want to mangle it.
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("true", "BOOLEAN")).isEqualTo("true")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("false", "BOOLEAN")).isEqualTo("false")
    }

    @Test
    fun `BOOLEAN dataType leaves unrecognised values untouched`() {
        // Defensive: garbage in → garbage out, but never silently cast to true/false.
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("yes", "BOOLEAN")).isEqualTo("yes")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("", "BOOLEAN")).isEqualTo("")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("2", "BOOLEAN")).isEqualTo("2")
    }

    @Test
    fun `DOUBLE dataType is never normalised - 1 stays 1, not true`() {
        // Critical: a sensor reading of 1.0 (e.g. 1 amp, 1 lux) must never become "true".
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", "DOUBLE")).isEqualTo("1")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("0", "DOUBLE")).isEqualTo("0")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1.5", "DOUBLE")).isEqualTo("1.5")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("185", "DOUBLE")).isEqualTo("185")
    }

    @Test
    fun `INTEGER and STRING dataTypes are never normalised`() {
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", "INTEGER")).isEqualTo("1")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("0", "INTEGER")).isEqualTo("0")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("hello", "STRING")).isEqualTo("hello")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", "LONG")).isEqualTo("1")
    }

    @Test
    fun `null dataType means non-boolean - pass-through`() {
        // Devices/settings without a catalogued type fall back to raw transport.
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("1", null)).isEqualTo("1")
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue("true", null)).isEqualTo("true")
    }

    @Test
    fun `null storedValue stays null regardless of dataType`() {
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue(null, "BOOLEAN")).isNull()
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue(null, "DOUBLE")).isNull()
        assertThat(GreenhouseStatusAssembler.normalizeCurrentValue(null, null)).isNull()
    }
}
