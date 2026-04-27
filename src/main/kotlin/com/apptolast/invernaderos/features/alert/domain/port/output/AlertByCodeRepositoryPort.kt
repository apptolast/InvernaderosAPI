package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert

interface AlertByCodeRepositoryPort {
    fun findByCode(code: String): Alert?
    fun save(alert: Alert): Alert
}
