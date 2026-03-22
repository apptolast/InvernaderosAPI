package com.apptolast.invernaderos.features.user.domain.port.output

interface PasswordHasher {
    fun hash(raw: String): String
}
