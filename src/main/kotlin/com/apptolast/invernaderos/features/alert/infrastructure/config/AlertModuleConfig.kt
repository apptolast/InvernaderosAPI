package com.apptolast.invernaderos.features.alert.infrastructure.config

import com.apptolast.invernaderos.features.alert.application.usecase.AlertActiveDurationStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.AlertByActorStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.AlertMttrStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.AlertRecurrenceStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.AlertSummaryStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.AlertTimeseriesStatsUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.ApplyAlertMqttSignalUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.CreateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.DeleteAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.FindAlertEpisodesUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.FindAlertHistoryUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.FindAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.ResolveAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.application.usecase.UpdateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertActiveDurationStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertByActorStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertMttrStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertRecurrenceStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertSummaryStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertTimeseriesStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.ApplyAlertMqttSignalUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.DeleteAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertEpisodesUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertHistoryUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertCodeGenerator
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSectorValidationPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSignalDecisionPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort
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

    // -------------------------------------------------------------------
    // History use cases
    // -------------------------------------------------------------------

    @Bean
    fun findAlertHistoryUseCase(
        historyQueryPort: AlertHistoryQueryPort,
    ): FindAlertHistoryUseCase = FindAlertHistoryUseCaseImpl(historyQueryPort)

    @Bean
    fun findAlertEpisodesUseCase(
        historyQueryPort: AlertHistoryQueryPort,
    ): FindAlertEpisodesUseCase = FindAlertEpisodesUseCaseImpl(historyQueryPort)

    // -------------------------------------------------------------------
    // Stats use cases
    // -------------------------------------------------------------------

    @Bean
    fun alertRecurrenceStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertRecurrenceStatsUseCase = AlertRecurrenceStatsUseCaseImpl(statsPort)

    @Bean
    fun alertMttrStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertMttrStatsUseCase = AlertMttrStatsUseCaseImpl(statsPort)

    @Bean
    fun alertTimeseriesStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertTimeseriesStatsUseCase = AlertTimeseriesStatsUseCaseImpl(statsPort)

    @Bean
    fun alertActiveDurationStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertActiveDurationStatsUseCase = AlertActiveDurationStatsUseCaseImpl(statsPort)

    @Bean
    fun alertByActorStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertByActorStatsUseCase = AlertByActorStatsUseCaseImpl(statsPort)

    @Bean
    fun alertSummaryStatsUseCase(
        statsPort: AlertStatsQueryPort,
    ): AlertSummaryStatsUseCase = AlertSummaryStatsUseCaseImpl(statsPort)
}
