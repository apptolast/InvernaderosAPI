package com.apptolast.invernaderos.features.command.domain.port.output

interface UserLookupPort {
    fun findUserByEmail(email: String): UserInfo?
}

data class UserInfo(val userId: Long, val tenantId: Long)
