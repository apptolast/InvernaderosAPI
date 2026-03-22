package com.apptolast.invernaderos.features.command.domain.port.input

import com.apptolast.invernaderos.features.command.domain.error.CommandError
import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.shared.domain.Either

interface SendCommandUseCase {
    fun execute(command: SendDeviceCommand): Either<CommandError, DeviceCommand>
}

data class SendDeviceCommand(
    val code: String,
    val value: String
)
