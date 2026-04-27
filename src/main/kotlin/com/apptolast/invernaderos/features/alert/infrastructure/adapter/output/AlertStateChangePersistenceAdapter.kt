package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertStateChangeRepository
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AlertStateChangePersistenceAdapter(
    private val repository: AlertStateChangeRepository
) : AlertStateChangePersistencePort {

    @Transactional("metadataTransactionManager", rollbackFor = [Exception::class])
    override fun save(change: AlertStateChange): AlertStateChange =
        repository.save(change.toEntity()).toDomain()
}
