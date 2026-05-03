package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.catalog.AlertSeverityRepository
import com.apptolast.invernaderos.features.notification.domain.port.output.AlertSeverityLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationSeveritySnapshot
import org.springframework.stereotype.Component

@Component
class AlertSeverityLookupAdapter(
    private val alertSeverityRepository: AlertSeverityRepository
) : AlertSeverityLookupPort {

    override fun findById(severityId: Short): NotificationSeveritySnapshot? =
        alertSeverityRepository.findById(severityId).orElse(null)?.let { severity ->
            NotificationSeveritySnapshot(
                id = severity.id!!,
                name = severity.name,
                level = severity.level,
                color = severity.color,
                notifyPush = severity.notifyPush
            )
        }
}
