package com.apptolast.invernaderos.features.command.application.usecase

import com.apptolast.invernaderos.features.command.domain.error.CommandError
import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.input.SendDeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.input.SendCommandUseCase
import com.apptolast.invernaderos.features.command.domain.port.output.CodeExistencePort
import com.apptolast.invernaderos.features.command.domain.port.output.CommandPublisherPort
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import com.apptolast.invernaderos.features.shared.domain.Either
import org.slf4j.LoggerFactory
import java.time.Instant

class SendCommandUseCaseImpl(
    private val persistence: DeviceCommandPersistencePort,
    private val codeExistence: CodeExistencePort,
    private val publisher: CommandPublisherPort
) : SendCommandUseCase {

    private val logger = LoggerFactory.getLogger(SendCommandUseCaseImpl::class.java)

    override fun execute(command: SendDeviceCommand): Either<CommandError, DeviceCommand> {
        val deviceExists = codeExistence.existsDeviceByCode(command.code)
        val settingExists = codeExistence.existsSettingByCode(command.code)

        if (!deviceExists && !settingExists) {
            return Either.Left(CommandError.CodeNotFound(command.code))
        }

        val deviceCommand = DeviceCommand(
            time = Instant.now(),
            code = command.code,
            value = command.value
        )

        val saved = persistence.save(deviceCommand)
        logger.info("Command persisted: code={}, value={}, time={}", saved.code, saved.value, saved.time)

        publisher.publish(command.code, command.value)
        logger.info("Command published to MQTT: code={}, value={}", command.code, command.value)

        return Either.Right(saved)
    }
}
