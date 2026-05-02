package com.apptolast.invernaderos.features.auth.refresh.infrastructure.adapter.output

import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Component
class Sha256OpaqueTokenGenerator : OpaqueTokenGenerator {

    private val rnd = SecureRandom()

    override fun generate(): String {
        val buf = ByteArray(32)
        rnd.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    override fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
