package com.apptolast.invernaderos.features.suggestion.domain.model

@JvmInline
value class SuggestionCategory(val value: String) {
    init {
        require(value.isNotBlank()) { "Suggestion category must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "Suggestion category exceeds $MAX_LENGTH characters (was ${value.length})"
        }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
