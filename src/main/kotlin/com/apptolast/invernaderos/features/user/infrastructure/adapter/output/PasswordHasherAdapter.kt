package com.apptolast.invernaderos.features.user.infrastructure.adapter.output

import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordHasherAdapter(
    private val passwordEncoder: PasswordEncoder
) : PasswordHasher {

    override fun hash(raw: String): String = passwordEncoder.encode(raw)
}
