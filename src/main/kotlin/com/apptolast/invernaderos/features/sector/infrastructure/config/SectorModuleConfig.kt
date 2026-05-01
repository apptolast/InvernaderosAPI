package com.apptolast.invernaderos.features.sector.infrastructure.config

import com.apptolast.invernaderos.features.sector.application.usecase.CreateSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.application.usecase.DeleteSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.application.usecase.FindSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.application.usecase.UpdateSectorUseCaseImpl
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.DeleteSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.FindSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorCodeGenerator
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SectorModuleConfig {

    @Bean
    fun createSectorUseCase(
        repository: SectorRepositoryPort,
        codeGenerator: SectorCodeGenerator,
        greenhouseExistence: GreenhouseExistencePort,
        applicationEventPublisher: ApplicationEventPublisher
    ): CreateSectorUseCase = CreateSectorUseCaseImpl(repository, codeGenerator, greenhouseExistence, applicationEventPublisher)

    @Bean
    fun findSectorUseCase(
        repository: SectorRepositoryPort
    ): FindSectorUseCase = FindSectorUseCaseImpl(repository)

    @Bean
    fun updateSectorUseCase(
        repository: SectorRepositoryPort,
        greenhouseExistence: GreenhouseExistencePort,
        applicationEventPublisher: ApplicationEventPublisher
    ): UpdateSectorUseCase = UpdateSectorUseCaseImpl(repository, greenhouseExistence, applicationEventPublisher)

    @Bean
    fun deleteSectorUseCase(
        repository: SectorRepositoryPort,
        applicationEventPublisher: ApplicationEventPublisher
    ): DeleteSectorUseCase = DeleteSectorUseCaseImpl(repository, applicationEventPublisher)
}
