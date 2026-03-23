package com.apptolast.invernaderos.features.command.domain.error

sealed interface CommandError {
    val message: String

    data class CodeNotFound(val code: String) : CommandError {
        override val message: String
            get() = "Code '$code' not found in devices or settings"
    }
}
