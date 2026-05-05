package com.apptolast.invernaderos.features.suggestion.domain.model

@JvmInline
value class SuggestionTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "Suggestion title must not be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Suggestion title length must be in $MIN_LENGTH..$MAX_LENGTH (was ${value.length})"
        }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 200
    }
}
