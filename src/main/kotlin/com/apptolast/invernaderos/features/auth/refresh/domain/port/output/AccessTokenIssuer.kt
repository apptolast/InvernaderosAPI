package com.apptolast.invernaderos.features.auth.refresh.domain.port.output

interface AccessTokenIssuer {
    fun issue(username: String, extraClaims: Map<String, Any>): String
    fun accessTtlSeconds(): Long
}
