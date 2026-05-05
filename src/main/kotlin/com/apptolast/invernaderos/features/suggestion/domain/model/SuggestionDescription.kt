package com.apptolast.invernaderos.features.suggestion.domain.model

@JvmInline
value class SuggestionDescription(val value: String) {
    init {
        require(value.isNotBlank()) { "Suggestion description must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "Suggestion description exceeds $MAX_LENGTH characters (was ${value.length})"
        }
    }

    companion object {
        const val MAX_LENGTH = 5000
    }
}
