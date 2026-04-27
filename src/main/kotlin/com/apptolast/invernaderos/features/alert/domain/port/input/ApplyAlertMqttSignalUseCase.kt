package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.shared.domain.Either

interface ApplyAlertMqttSignalUseCase {
    fun execute(signal: AlertMqttSignal): Either<AlertError, AlertSignalApplied>
}

data class AlertSignalApplied(
    val alert: Alert,
    val change: AlertStateChange?   // null when the decision was NO_OP
)
