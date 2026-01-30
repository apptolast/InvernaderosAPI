package com.apptolast.invernaderos.features.user

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @EntityGraph(value = "User.tenant") fun findByUsername(username: String): User?
    @EntityGraph(value = "User.tenant") fun findByEmail(email: String): User?
    @EntityGraph(value = "User.tenant") fun findByTenantId(tenantId: Long): List<User>
    @EntityGraph(value = "User.tenant") fun findByIdAndTenantId(id: Long, tenantId: Long): User?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByResetPasswordToken(token: String): User?
}
