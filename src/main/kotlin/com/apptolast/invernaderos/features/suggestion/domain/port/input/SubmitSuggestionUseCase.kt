package com.apptolast.invernaderos.features.suggestion.domain.port.input

import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotificationMeta

interface SubmitSuggestionUseCase {
    fun execute(command: SubmitSuggestionCommand): SubmitSuggestionResult
}

data class SubmitSuggestionCommand(
    val suggestion: Suggestion,
    val meta: SuggestionNotificationMeta
)
