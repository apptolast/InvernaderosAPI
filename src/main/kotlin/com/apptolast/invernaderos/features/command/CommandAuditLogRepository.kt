package com.apptolast.invernaderos.features.command

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommandAuditLogRepository : JpaRepository<CommandAuditLog, Long> {
    fun findByUserIdOrderByTimeDesc(userId: Long): List<CommandAuditLog>
    fun findByTenantIdOrderByTimeDesc(tenantId: Long): List<CommandAuditLog>
    fun findBySettingCodeOrderByTimeDesc(settingCode: String): List<CommandAuditLog>
}
