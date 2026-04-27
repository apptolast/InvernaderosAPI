package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertStateChangeRepository
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import org.springframework.stereotype.Component

@Component
class AlertStateChangePersistenceAdapter(
    private val repository: AlertStateChangeRepository
) : AlertStateChangePersistencePort {

    // Transaction boundary lives in AlertMqttInboundAdapter.handleSignal so that the
    // alert update and this state-change write share a single tx. Adding @Transactional
    // here would invite future drift (e.g. REQUIRES_NEW) that would silently break atomicity.
    override fun save(change: AlertStateChange): AlertStateChange =
        repository.save(change.toEntity()).toDomain()
}
