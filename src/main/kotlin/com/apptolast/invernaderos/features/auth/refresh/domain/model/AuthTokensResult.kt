package com.apptolast.invernaderos.features.auth.refresh.domain.model

data class AuthTokensResult(
    val accessToken: String,
    val refreshToken: String,           // plaintext, ONLY here, never persisted
    val accessTtlSeconds: Long,
    val refreshTtlSeconds: Long,
    val username: String,
    val roles: List<String>
)
