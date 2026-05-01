package com.apptolast.invernaderos.features.greenhouse.infrastructure.config

import com.apptolast.invernaderos.features.greenhouse.application.usecase.CreateGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.application.usecase.DeleteGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.application.usecase.FindGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.application.usecase.UpdateGreenhouseUseCaseImpl
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.DeleteGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.FindGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseCodeGenerator
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GreenhouseModuleConfig {

    @Bean
    fun createGreenhouseUseCase(
        repository: GreenhouseRepositoryPort,
        codeGenerator: GreenhouseCodeGenerator,
        applicationEventPublisher: ApplicationEventPublisher
    ): CreateGreenhouseUseCase = CreateGreenhouseUseCaseImpl(repository, codeGenerator, applicationEventPublisher)

    @Bean
    fun findGreenhouseUseCase(
        repository: GreenhouseRepositoryPort
    ): FindGreenhouseUseCase = FindGreenhouseUseCaseImpl(repository)

    @Bean
    fun updateGreenhouseUseCase(
        repository: GreenhouseRepositoryPort,
        applicationEventPublisher: ApplicationEventPublisher
    ): UpdateGreenhouseUseCase = UpdateGreenhouseUseCaseImpl(repository, applicationEventPublisher)

    @Bean
    fun deleteGreenhouseUseCase(
        repository: GreenhouseRepositoryPort,
        applicationEventPublisher: ApplicationEventPublisher
    ): DeleteGreenhouseUseCase = DeleteGreenhouseUseCaseImpl(repository, applicationEventPublisher)
}
