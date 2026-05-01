package com.apptolast.invernaderos.features.auth.refresh.domain.port.output

interface UserClaimsLookup {
    fun loadForToken(userId: Long): UserClaimsSnapshot

    data class UserClaimsSnapshot(
        val username: String,
        val roles: List<String>,
        val claims: Map<String, Any>     // tenantId, role
    )
}
