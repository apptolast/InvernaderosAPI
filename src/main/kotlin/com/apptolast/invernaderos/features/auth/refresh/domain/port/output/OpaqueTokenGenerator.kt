package com.apptolast.invernaderos.features.auth.refresh.domain.port.output

interface OpaqueTokenGenerator {
    fun generate(): String          // 32-byte SecureRandom, base64url no-padding
    fun hash(token: String): String // SHA-256 hex (lowercase, 64 chars)
}
