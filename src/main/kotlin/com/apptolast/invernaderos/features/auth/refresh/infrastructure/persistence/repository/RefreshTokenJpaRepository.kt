package com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.repository

import com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.entity.RefreshTokenEntity
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.tokenHash = :h")
    fun lockByTokenHash(@Param("h") h: String): RefreshTokenEntity?

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :at WHERE r.id = :id AND r.revokedAt IS NULL")
    fun revokeById(@Param("id") id: Long, @Param("at") at: Instant): Int

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :at WHERE r.familyId = :fid AND r.revokedAt IS NULL")
    fun revokeFamily(@Param("fid") fid: UUID, @Param("at") at: Instant): Int

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :at WHERE r.userId = :uid AND r.revokedAt IS NULL")
    fun revokeAllActiveByUser(@Param("uid") uid: Long, @Param("at") at: Instant): Int
}
