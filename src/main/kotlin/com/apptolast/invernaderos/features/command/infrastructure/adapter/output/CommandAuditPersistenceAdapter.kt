package com.apptolast.invernaderos.features.command.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.command.CommandAuditLog
import com.apptolast.invernaderos.features.command.CommandAuditLogRepository
import com.apptolast.invernaderos.features.command.domain.port.output.CommandAuditPersistencePort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class CommandAuditPersistenceAdapter(
    private val repository: CommandAuditLogRepository,
    private val codeGenerator: CodeGeneratorService
) : CommandAuditPersistencePort {

    @Transactional("metadataTransactionManager")
    override fun save(time: Instant, settingCode: String, value: String, userId: Long, tenantId: Long) {
        val auditLog = CommandAuditLog(
            code = codeGenerator.generateAuditCode(),
            time = time,
            settingCode = settingCode,
            value = value,
            userId = userId,
            tenantId = tenantId
        )
        repository.save(auditLog)
    }
}
