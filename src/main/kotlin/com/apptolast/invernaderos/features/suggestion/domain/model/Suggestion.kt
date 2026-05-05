package com.apptolast.invernaderos.features.suggestion.domain.model

data class Suggestion(
    val category: SuggestionCategory,
    val title: SuggestionTitle,
    val description: SuggestionDescription
)
