package com.apptolast.invernaderos.features.alert.infrastructure.config

import com.apptolast.invernaderos.features.alert.application.usecase.ApplyAlertMqttSignalUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.CreateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.DeleteAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.FindAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.ResolveAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.UpdateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.port.input.ApplyAlertMqttSignalUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.DeleteAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertCodeGenerator
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSectorValidationPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSignalDecisionPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AlertMqttProperties::class)
class AlertModuleConfig {

    @Bean
    fun createAlertUseCase(
        repository: AlertRepositoryPort,
        codeGenerator: AlertCodeGenerator,
        sectorValidation: AlertSectorValidationPort
    ): CreateAlertUseCase = CreateAlertUseCaseImpl(repository, codeGenerator, sectorValidation)

    @Bean
    fun findAlertUseCase(
        repository: AlertRepositoryPort
    ): FindAlertUseCase = FindAlertUseCaseImpl(repository)

    @Bean
    fun updateAlertUseCase(
        repository: AlertRepositoryPort,
        sectorValidation: AlertSectorValidationPort
    ): UpdateAlertUseCase = UpdateAlertUseCaseImpl(repository, sectorValidation)

    @Bean
    fun deleteAlertUseCase(
        repository: AlertRepositoryPort
    ): DeleteAlertUseCase = DeleteAlertUseCaseImpl(repository)

    @Bean
    fun resolveAlertUseCase(
        repository: AlertRepositoryPort,
        stateChangePort: AlertStateChangePersistencePort,
        eventPublisher: AlertStateChangedEventPublisherPort
    ): ResolveAlertUseCase = ResolveAlertUseCaseImpl(repository, stateChangePort, eventPublisher)

    @Bean
    fun applyAlertMqttSignalUseCase(
        alertByCodeRepository: AlertByCodeRepositoryPort,
        decisionPort: AlertSignalDecisionPort,
        stateChangePort: AlertStateChangePersistencePort,
        eventPublisher: AlertStateChangedEventPublisherPort
    ): ApplyAlertMqttSignalUseCase = ApplyAlertMqttSignalUseCaseImpl(
        alertByCodeRepository = alertByCodeRepository,
        decisionPort = decisionPort,
        stateChangePort = stateChangePort,
        eventPublisher = eventPublisher
    )
}
