package com.apptolast.invernaderos.features.command.domain.port.output

import java.time.Instant

interface CommandAuditPersistencePort {
    fun save(time: Instant, settingCode: String, value: String, userId: Long, tenantId: Long)
}
