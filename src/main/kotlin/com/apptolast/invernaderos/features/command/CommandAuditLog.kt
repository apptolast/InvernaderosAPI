package com.apptolast.invernaderos.features.command

import com.apptolast.invernaderos.features.user.User
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "command_audit_log",
    schema = "metadata",
    indexes = [
        Index(name = "idx_command_audit_log_user", columnList = "user_id"),
        Index(name = "idx_command_audit_log_tenant", columnList = "tenant_id"),
        Index(name = "idx_command_audit_log_time", columnList = "time"),
        Index(name = "idx_command_audit_log_setting_code", columnList = "setting_code"),
        Index(name = "idx_command_audit_log_code", columnList = "code")
    ]
)
data class CommandAuditLog(
    @Id
    @Tsid
    var id: Long? = null,

    @Column(nullable = false, length = 50, unique = true)
    var code: String,

    @Column(nullable = false)
    val time: Instant,

    @Column(name = "setting_code", nullable = false, length = 20)
    val settingCode: String,

    @Column(nullable = false, length = 100)
    val value: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var user: User? = null

    override fun toString(): String {
        return "CommandAuditLog(id=$id, settingCode='$settingCode', value='$value', userId=$userId, time=$time)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandAuditLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
