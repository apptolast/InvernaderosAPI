package com.apptolast.invernaderos.features.user

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    @EntityGraph(value = "User.tenant") fun findByUsername(username: String): User?
    @EntityGraph(value = "User.tenant") fun findByEmail(email: String): User?
    @EntityGraph(value = "User.tenant") fun findByTenantId(tenantId: UUID): List<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}
