package com.apptolast.invernaderos.features.auth.refresh.domain.error

import java.util.UUID

sealed interface AuthError {
    val message: String

    data object MalformedToken : AuthError {
        override val message = "Malformed refresh token"
    }

    data object TokenNotFound : AuthError {
        override val message = "Refresh token not found"
    }

    data object TokenExpired : AuthError {
        override val message = "Refresh token expired"
    }

    data object TokenRevoked : AuthError {
        override val message = "Refresh token revoked"
    }

    data class TokenReuseDetected(val familyId: UUID, val userId: Long) : AuthError {
        override val message = "Refresh token reuse detected for user $userId family $familyId"
    }

    data object FeatureDisabled : AuthError {
        override val message = "Refresh token feature is disabled"
    }
}
