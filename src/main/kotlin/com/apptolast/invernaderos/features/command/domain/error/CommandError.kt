package com.apptolast.invernaderos.features.command.domain.error

sealed interface CommandError {
    val message: String

    data class CodeNotFound(val code: String) : CommandError {
        override val message: String
            get() = "Code '$code' not found in devices or settings"
    }

    data class UserNotFound(val email: String) : CommandError {
        override val message: String
            get() = "User with email '$email' not found"
    }
}
