package com.apptolast.invernaderos.features.push

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PushTokenRepository : JpaRepository<PushToken, Long> {

    fun findByToken(token: String): PushToken?

    fun findAllByTenantId(tenantId: Long): List<PushToken>

    @Modifying
    @Query("DELETE FROM PushToken p WHERE p.token = :token")
    fun deleteByToken(@Param("token") token: String): Int
}
