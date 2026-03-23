package com.apptolast.invernaderos.features.command.infrastructure.config

import com.apptolast.invernaderos.features.command.application.usecase.QueryCommandHistoryUseCaseImpl
import com.apptolast.invernaderos.features.command.application.usecase.SendCommandUseCaseImpl
import com.apptolast.invernaderos.features.command.domain.port.input.QueryCommandHistoryUseCase
import com.apptolast.invernaderos.features.command.domain.port.input.SendCommandUseCase
import com.apptolast.invernaderos.features.command.domain.port.output.CodeExistencePort
import com.apptolast.invernaderos.features.command.domain.port.output.CommandPublisherPort
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommandModuleConfig {

    @Bean
    fun sendCommandUseCase(
        persistence: DeviceCommandPersistencePort,
        codeExistence: CodeExistencePort,
        publisher: CommandPublisherPort
    ): SendCommandUseCase = SendCommandUseCaseImpl(persistence, codeExistence, publisher)

    @Bean
    fun queryCommandHistoryUseCase(
        persistence: DeviceCommandPersistencePort
    ): QueryCommandHistoryUseCase = QueryCommandHistoryUseCaseImpl(persistence)
}
