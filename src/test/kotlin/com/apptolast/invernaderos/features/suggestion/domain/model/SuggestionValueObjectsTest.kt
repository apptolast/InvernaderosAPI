package com.apptolast.invernaderos.features.suggestion.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SuggestionValueObjectsTest {

    // --- SuggestionCategory ---

    @Test
    fun `SuggestionCategory accepts valid values`() {
        assertEquals("Sugerencia", SuggestionCategory("Sugerencia").value)
    }

    @Test
    fun `SuggestionCategory rejects blank`() {
        assertThrows<IllegalArgumentException> { SuggestionCategory("") }
        assertThrows<IllegalArgumentException> { SuggestionCategory("   ") }
    }

    @Test
    fun `SuggestionCategory rejects values longer than 50 chars`() {
        assertThrows<IllegalArgumentException> { SuggestionCategory("a".repeat(51)) }
    }

    // --- SuggestionTitle ---

    @Test
    fun `SuggestionTitle accepts values within 3 to 200 chars`() {
        assertEquals("abc", SuggestionTitle("abc").value)
        assertEquals("a".repeat(200), SuggestionTitle("a".repeat(200)).value)
    }

    @Test
    fun `SuggestionTitle rejects blank or too short or too long`() {
        assertThrows<IllegalArgumentException> { SuggestionTitle("") }
        assertThrows<IllegalArgumentException> { SuggestionTitle("ab") }
        assertThrows<IllegalArgumentException> { SuggestionTitle("a".repeat(201)) }
    }

    // --- SuggestionDescription ---

    @Test
    fun `SuggestionDescription accepts up to 5000 chars`() {
        assertEquals("a".repeat(5000), SuggestionDescription("a".repeat(5000)).value)
    }

    @Test
    fun `SuggestionDescription rejects blank or too long`() {
        assertThrows<IllegalArgumentException> { SuggestionDescription("") }
        assertThrows<IllegalArgumentException> { SuggestionDescription("a".repeat(5001)) }
    }
}
