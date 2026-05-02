package com.apptolast.invernaderos.features.auth.dto.mapper

import com.apptolast.invernaderos.features.auth.dto.response.JwtResponse
import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult

fun AuthTokensResult.toJwtResponse(): JwtResponse = JwtResponse(
    token = accessToken,
    username = username,
    roles = roles,
    refreshToken = refreshToken,
    expiresIn = accessTtlSeconds,
    refreshExpiresIn = refreshTtlSeconds
)
