package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange

interface AlertStateChangePersistencePort {
    fun save(change: AlertStateChange): AlertStateChange
}
