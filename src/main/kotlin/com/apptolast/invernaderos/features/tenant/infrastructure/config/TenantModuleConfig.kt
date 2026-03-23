package com.apptolast.invernaderos.features.tenant.infrastructure.config

import com.apptolast.invernaderos.features.tenant.application.usecase.CreateTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.application.usecase.DeleteTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.application.usecase.FindTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.application.usecase.UpdateTenantUseCaseImpl
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.DeleteTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.FindTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantCodeGenerator
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TenantModuleConfig {

    @Bean
    fun createTenantUseCase(
        repository: TenantRepositoryPort,
        codeGenerator: TenantCodeGenerator
    ): CreateTenantUseCase = CreateTenantUseCaseImpl(repository, codeGenerator)

    @Bean
    fun findTenantUseCase(
        repository: TenantRepositoryPort
    ): FindTenantUseCase = FindTenantUseCaseImpl(repository)

    @Bean
    fun updateTenantUseCase(
        repository: TenantRepositoryPort
    ): UpdateTenantUseCase = UpdateTenantUseCaseImpl(repository)

    @Bean
    fun deleteTenantUseCase(
        repository: TenantRepositoryPort
    ): DeleteTenantUseCase = DeleteTenantUseCaseImpl(repository)
}
